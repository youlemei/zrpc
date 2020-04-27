package com.lwz.registry;

import com.alibaba.fastjson.JSON;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.SingleThreadEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.AddWatchMode;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author liweizhou 2020/4/17
 */
@Slf4j
public class ZooKeeperRegistrar implements Registrar {

    private RegistryProperties registryProperties;

    private ServerInfo serverInfo;

    private String uuid;

    private ZooKeeper zooKeeper;

    private volatile List<ServerInfo> serverInfos;

    private SingleThreadEventExecutor watchExecutor;

    /**
     * 客户端初始化
     *
     * @param registryProperties
     */
    public ZooKeeperRegistrar(RegistryProperties registryProperties) {
        this.registryProperties = registryProperties;
        initZookeeper();
        initWatch();
    }

    /**
     * 服务器初始化
     *
     * @param registryProperties
     * @param serverInfo
     */
    public ZooKeeperRegistrar(RegistryProperties registryProperties, ServerInfo serverInfo) {
        this.registryProperties = registryProperties;
        this.serverInfo = serverInfo;
        this.uuid = UUID.randomUUID().toString();
        initZookeeper();
    }

    private void initZookeeper() {
        try {
            CountDownLatch initZookeeper = new CountDownLatch(1);
            zooKeeper = new ZooKeeper(registryProperties.getRegistryUrl(), 10000, event->{
                if (KeeperState.SyncConnected == event.getState()) {
                    log.info("initZookeeper success. zookeeper:{}", registryProperties.getRegistryUrl());
                    initZookeeper.countDown();
                }
            });
            Assert.isTrue(initZookeeper.await(10, TimeUnit.SECONDS), "timeout");
        } catch (Exception e) {
            log.warn("initZookeeper fail. zookeeper:{} err:{}", registryProperties.getRegistryUrl(), e.getMessage(), e);
            throw new BeanCreationException(String.format("initZookeeper fail. zookeeper:%s err:%s",
                    registryProperties.getRegistryUrl(), e.getMessage()));
        }
    }

    private void initWatch() {
        watchExecutor = new DefaultEventExecutor();
        String path = registryProperties.getRootPath() + "/" + registryProperties.getServerName();
        getServerChildren(path);
        zooKeeper.addWatch(path, event -> {
            getServerChildren(path);
        }, AddWatchMode.PERSISTENT_RECURSIVE, (int rc, String p, Object ctx)->{
            Code code = Code.get(rc);
            if (Code.OK == code) {
                log.info("initWatch success. server:{}", p);
            } else {
                //TODO: 失败重试
                log.error("initWatch fail. server:{} code:{}", p, code);
            }
        }, this);
    }

    private void getServerChildren(String path) {
        zooKeeper.getChildren(path, false, (int rc, String p, Object ctx, List<String> children, Stat stat) -> {
            watchExecutor.execute(()-> getChildrenData(path, Optional.ofNullable(children).orElse(Collections.emptyList())));
        }, this);
    }

    private void getChildrenData(String path, List<String> children) {
        CountDownLatch countDownLatch = new CountDownLatch(children.size());
        List<ServerInfo> serverInfos = new CopyOnWriteArrayList<>();
        children.forEach(child -> {
            zooKeeper.getData(path + "/" + child, false, (int rc, String p, Object ctx, byte[] data, Stat stat) -> {
                try {
                    Code code = Code.get(rc);
                    String text = new String(data);
                    log.info("serverInfo code:{} path:{} data:{}", code, p, text);
                    if (Code.OK == code) {
                        serverInfos.add(JSON.parseObject(text, ServerInfo.class));
                    }
                } finally {
                    countDownLatch.countDown();
                }
            }, this);
        });

        try {
            countDownLatch.await();
            if (this.serverInfos == null) {
                synchronized (this) {
                    if (this.serverInfos == null) {
                        this.serverInfos = serverInfos;
                        log.info("serverInfos init ok. serverInfos:{}", serverInfos);
                        this.notifyAll();
                        return;
                    }
                }
            }
            this.serverInfos = serverInfos;
        } catch (InterruptedException e) {
            //ignore
        }
    }

    @Override
    public List<ServerInfo> getServerInfos() {
        if (serverInfos == null) {
            synchronized (this) {
                if (serverInfos == null) {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        //ignore
                    }
                }
            }
        }
        return serverInfos;
    }

    @Override
    public void signIn() {
        String path = registryProperties.getRootPath() + "/" + registryProperties.getServerName() + "/" + uuid;
        createServerNode(path, 0);
    }

    private void createServerNode(String path, int offset) {
        int index = path.indexOf("/", offset + 1);
        if (index > 0) {
            String parent = path.substring(0, index);
            zooKeeper.create(parent, null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT,
                    (int rc, String p, Object ctx, String name, Stat stat) -> {
                        Code code = Code.get(rc);
                        log.info("createParent code:{} node:{}", code, p);
                        createServerNode(path, index);
                    }, this);
        } else {
            byte[] data = JSON.toJSONString(serverInfo).getBytes();
            zooKeeper.create(path, data, Ids.READ_ACL_UNSAFE, CreateMode.EPHEMERAL,
                    (int rc, String p, Object ctx, String name, Stat stat)->{
                        Code code = Code.get(rc);
                        switch (code) {
                            case OK:
                            case NODEEXISTS:
                                log.info("signIn success. zookeeper:{} node:{}", registryProperties.getRegistryUrl(), path);
                                break;
                            default:
                                log.error("signIn fail. zookeeper:{} code:{} node:{}", registryProperties.getRegistryUrl(), code, path);
                                //TODO: 重试策略
                                break;
                        }
                    }, this);
        }
    }

    @Override
    public void signOut() {
        try {
            if (zooKeeper != null) {
                zooKeeper.close();
                log.info("signOut success. zookeeper:{} node:{}", registryProperties.getRegistryUrl(), uuid);
            }
        } catch (Exception e) {
            log.warn("zookeeper close err:{}", e.getMessage(), e);
        } finally {
            if (watchExecutor != null) {
                watchExecutor.shutdownGracefully();
            }
        }
    }

}

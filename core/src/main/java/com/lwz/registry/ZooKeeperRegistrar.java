package com.lwz.registry;

import com.alibaba.fastjson.JSON;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.SingleThreadEventExecutor;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * @author liweizhou 2020/4/17
 */
public class ZooKeeperRegistrar implements Registrar {

    private static final Logger log = LoggerFactory.getLogger(ZooKeeperRegistrar.class);

    private RegistryProperties registryProperties;

    private ZooKeeper zooKeeper;

    private ConcurrentMap<String, Consumer<List<ServerInfo>>> listenerMap = new ConcurrentHashMap<>();

    private ConcurrentMap<String, ServerInfo> registerMap = new ConcurrentHashMap<>();

    private SingleThreadEventExecutor zookeeperExecutor = new DefaultEventExecutor();

    public ZooKeeperRegistrar(RegistryProperties registryProperties) {
        this.registryProperties = registryProperties;
        CountDownLatch initZookeeper = new CountDownLatch(1);
        initZookeeper(() -> initZookeeper.countDown());
        try {
            Assert.isTrue(initZookeeper.await(initZookeeperTimeout.getSeconds(), TimeUnit.SECONDS), "zookeeper init fail");;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //默认配置
    int zookeeperSessionTimeout = 10000;
    Duration initZookeeperTimeout = Duration.ofSeconds(5);

    private void initZookeeper(Runnable task) {
        try {
            zooKeeper = new ZooKeeper(registryProperties.getRegistryUrl(), zookeeperSessionTimeout, event -> {
                log.info("zookeeper event:{}", event);
                switch (event.getState()) {
                    case SyncConnected:
                        log.info("ZooKeeper SyncConnected. {}", registryProperties.getRegistryUrl());
                        task.run();
                        break;
                    case Expired:
                        log.info("ZooKeeper Expired. {}", registryProperties.getRegistryUrl());
                        //重新连接,重新注册watch
                        zookeeperExecutor.execute(() -> {
                            try {
                                zooKeeper.close();
                            } catch (InterruptedException e) {
                                //ignore
                            }
                            AtomicBoolean run = new AtomicBoolean();
                            initZookeeper(() -> {
                                if (run.compareAndSet(false, true)) {
                                    registerMap.forEach((path, serverInfo) -> createServerNode(path, 0, serverInfo));
                                    listenerMap.keySet().forEach(serverName -> getServerChildren(zooKeeper, serverName));
                                }
                            });
                        });
                        break;
                    default:break;
                }
            });
        } catch (Exception e) {
            log.error("initZookeeper fail. zookeeper:{} err:{}", registryProperties.getRegistryUrl(), e.getMessage());
        }
    }

    @Override
    public void addListener(String serverName, Consumer<List<ServerInfo>> listener) {
        if (listenerMap.putIfAbsent(serverName, listener) == null) {
            getServerChildren(zooKeeper, serverName);
        }
    }

    private void getServerChildren(ZooKeeper zooKeeper, String serverName) {
        String serverPath = registryProperties.getRootPath() + "/" + serverName;
        this.zooKeeper.getChildren(serverPath, event -> getServerChildren(zooKeeper, serverName),
                (int rc, String p, Object ctx, List<String> children, Stat stat) -> {
                    Code code = Code.get(rc);
                    if (Code.OK == code && this.zooKeeper == zooKeeper) {
                        log.info("serverInfo:{} code:{} serverChildren:{}", p, code, children);
                        zookeeperExecutor.execute(() -> getChildrenData(serverPath, children, zooKeeper, serverName));
                    } else {
                        log.info("serverInfo:{} code:{} discard.", p, code);
                    }
                }, this);
    }

    private void getChildrenData(String serverPath, List<String> children, ZooKeeper zooKeeper, String serverName) {
        AtomicBoolean update = new AtomicBoolean(true);
        List<ServerInfo> serverInfos = new CopyOnWriteArrayList<>();
        if (!CollectionUtils.isEmpty(children)) {
            CountDownLatch countDownLatch = new CountDownLatch(children.size());
            children.forEach(child -> {
                this.zooKeeper.getData(serverPath + "/" + child, false, (int rc, String p, Object ctx, byte[] data, Stat stat) -> {
                    try {
                        Code code = Code.get(rc);
                        if (Code.OK == code && this.zooKeeper == zooKeeper) {
                            String text = new String(data);
                            log.info("serverInfo:{} code:{} data:{}", p, code, text);
                            serverInfos.add(JSON.parseObject(text, ServerInfo.class));
                        } else {
                            log.info("serverInfo:{} code:{} discard.", p, code);
                            update.set(false);
                        }
                    } finally {
                        countDownLatch.countDown();
                    }
                }, this);
            });
            try {
                Assert.isTrue(countDownLatch.await(10, TimeUnit.SECONDS));
            } catch (Exception e) {
                update.set(false);
            }
        }
        if (update.get()) {
            Consumer<List<ServerInfo>> listener = listenerMap.get(serverName);
            if (listener != null) {
                listener.accept(serverInfos);
            }
        }
    }

    @Override
    public void register(String serverName, ServerInfo serverInfo) {
        String path = registryProperties.getRootPath() + "/" + serverName + "/" + UUID.randomUUID();
        if (registerMap.putIfAbsent(path, serverInfo) == null) {
            createServerNode(path, 0, serverInfo);
        }
    }

    private void createServerNode(String path, int offset, ServerInfo serverInfo) {
        int index = path.indexOf("/", offset + 1);
        if (index > 0) {
            String parent = path.substring(0, index);
            zooKeeper.create(parent, null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT,
                    (int rc, String p, Object ctx, String name, Stat stat) -> {
                        Code code = Code.get(rc);
                        log.debug("createParent code:{} node:{}", code, p);
                        createServerNode(path, index, serverInfo);
                    }, this);
        } else {
            byte[] data = JSON.toJSONString(serverInfo).getBytes();
            zooKeeper.create(path, data, Ids.READ_ACL_UNSAFE, CreateMode.EPHEMERAL, (int rc, String p, Object ctx, String name, Stat stat) -> {
                Code code = Code.get(rc);
                switch (code) {
                    case OK:
                    case NODEEXISTS:
                        log.info("register success. zookeeper:{} node:{}", registryProperties.getRegistryUrl(), path);
                        break;
                    default:
                        log.error("register fail. zookeeper:{} code:{} node:{}", registryProperties.getRegistryUrl(), code, path);
                        break;
                }
            }, this);
        }
    }

    @Override
    public void unRegister() {
        try {
            if (zooKeeper != null) {
                zooKeeper.close();
                log.info("zookeeper close success. zookeeper:{}", registryProperties.getRegistryUrl());
            }
        } catch (Exception e) {
            log.warn("zookeeper close err:{}", e.getMessage(), e);
        } finally {
            zookeeperExecutor.shutdownGracefully();
        }
    }

}

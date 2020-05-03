package com.lwz.registry;

import com.alibaba.fastjson.JSON;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.SingleThreadEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.AddWatchMode;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author liweizhou 2020/4/17
 */
@Slf4j
public class ZooKeeperRegistrar implements Registrar {

    private RegistryProperties registryProperties;

    private ZooKeeper zooKeeper;

    private Consumer<List<ServerInfo>> listener;

    private Runnable signIn;

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
    Duration addZookeeperWatchTimeout = Duration.ofSeconds(5);

    private void initZookeeper(Runnable task) {
        try {
            if (zooKeeper != null) {
                zooKeeper.close();
            }

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
                            initZookeeper(() -> {
                                if (signIn != null) {
                                    signIn.run();
                                }
                                setListener(listener);
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
    public void setListener(Consumer<List<ServerInfo>> listener) {
        if (listener != null) {
            this.listener = listener;
            String path = registryProperties.getRootPath() + "/" + registryProperties.getServerName();
            getServerChildren(path, zooKeeper);
            addWatch(path);
        }
    }

    private void addWatch(String path) {
        ZooKeeper zooKeeper = this.zooKeeper;
        try {
            zooKeeper.addWatch(path, event -> getServerChildren(path, zooKeeper), AddWatchMode.PERSISTENT_RECURSIVE);
        } catch (Exception e) {
            log.error("initWatch fail. server:{} err:{}", path, e.getMessage());
            zookeeperExecutor.schedule(() -> {
                if (this.zooKeeper == zooKeeper) {
                    addWatch(path);
                }
            }, addZookeeperWatchTimeout.getSeconds(), TimeUnit.SECONDS);
        }
    }

    private void getServerChildren(String path, ZooKeeper zooKeeper) {
        this.zooKeeper.getChildren(path, false, (int rc, String p, Object ctx, List<String> children, Stat stat) -> {
            if (this.zooKeeper == zooKeeper) {
                Code code = Code.get(rc);
                log.info("serverInfo:{} code:{} serverChildren:{}", p, code, children);
                if (Code.OK == code) {
                    zookeeperExecutor.execute(() -> getChildrenData(path, Optional.ofNullable(children).orElse(Collections.emptyList())));
                }
            }
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
                    log.info("serverInfo:{} code:{} data:{}", p, code, text);
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
            listener.accept(serverInfos);
        } catch (InterruptedException e) {
            //ignore
        }
    }

    @Override
    public void signIn(ServerInfo serverInfo) {
        String path = registryProperties.getRootPath() + "/" + registryProperties.getServerName() + "/" + UUID.randomUUID();
        createServerNode(path, 0, serverInfo);
    }

    private void createServerNode(String path, int offset, ServerInfo serverInfo) {
        int index = path.indexOf("/", offset + 1);
        if (index > 0) {
            String parent = path.substring(0, index);
            zooKeeper.create(parent, null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT,
                    (int rc, String p, Object ctx, String name, Stat stat) -> {
                        Code code = Code.get(rc);
                        log.info("createParent code:{} node:{}", code, p);
                        createServerNode(path, index, serverInfo);
                    }, this);
        } else {
            Runnable signIn = () -> {
                byte[] data = JSON.toJSONString(serverInfo).getBytes();
                zooKeeper.create(path, data, Ids.READ_ACL_UNSAFE, CreateMode.EPHEMERAL,
                        (int rc, String p, Object ctx, String name, Stat stat) -> {
                            Code code = Code.get(rc);
                            switch (code) {
                                case OK:
                                case NODEEXISTS:
                                    log.info("signIn success. zookeeper:{} node:{}", registryProperties.getRegistryUrl(), path);
                                    break;
                                default:
                                    log.error("signIn fail. zookeeper:{} code:{} node:{}", registryProperties.getRegistryUrl(), code, path);
                                    break;
                            }
                        }, this);
            };
            this.signIn = signIn;
            signIn.run();
        }
    }

    @Override
    public void signOut() {
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

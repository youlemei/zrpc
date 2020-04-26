package com.lwz.registry;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.AddWatchMode;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.util.List;
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

    private ZooKeeper zooKeeper;

    private String uuid;

    private volatile boolean initZookeeper;

    private volatile boolean initWatch;

    private volatile List<ServerInfo> serverInfos;

    public ZooKeeperRegistrar(RegistryProperties registryProperties) {
        this.registryProperties = registryProperties;
        this.uuid = UUID.randomUUID().toString();
    }

    @Override
    public List<ServerInfo> getServerInfos() {
        initZookeeper();
        initWatch();
        while (!initWatch) {
            try {
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException e) {
                //ignore
            }
        }
        return serverInfos;
    }

    private void initWatch() {
        String path = registryProperties.getRootPath() + "/" + registryProperties.getServerName();
        if (!initWatch) {
            //synchronized(this){
            //    if (!initWatch) {
                    getServerInfos(path);
                //}
            //}
        }
        zooKeeper.addWatch(path, event -> {
            getServerInfos(path);
        }, AddWatchMode.PERSISTENT_RECURSIVE, (int rc, String p, Object ctx)->{
            //TODO: 异常处理
        }, this);
    }

    private void getServerInfos(String path) {
        zooKeeper.getChildren(path, false, (rc, p, ctx, children, stat) -> {
            //TODO: 初始化优化
            new Thread(() -> {
                List<ServerInfo> serverInfos = new CopyOnWriteArrayList<>();
                CountDownLatch countDownLatch = new CountDownLatch(children.size());
                children.forEach(child -> {
                    zooKeeper.getData(path + "/" + child, false, ((rc1, path2, ctx1, data, stat1) -> {
                        try {
                            Code code = Code.get(rc1);
                            log.info("getServerInfos code:{}", code);
                            if (Code.OK == code) {
                                serverInfos.add(JSON.parseObject(new String(data), ServerInfo.class));
                            }
                        } finally {
                            countDownLatch.countDown();
                        }
                    }), this);
                });
                try {
                    countDownLatch.await();
                    this.serverInfos = serverInfos;
                    if (!initWatch) {
                        initWatch = true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }, this);
    }

    @Override
    public void signIn() {
        initZookeeper();

        //TODO: 创建父节点
        String path = registryProperties.getRootPath() + "/" + registryProperties.getServerName() + "/" + uuid;
        ServerInfo serverInfo = new ServerInfo("localhost", 7320);
        byte[] data = JSON.toJSONString(serverInfo).getBytes();
        zooKeeper.create(path, data, Ids.READ_ACL_UNSAFE, CreateMode.EPHEMERAL,
                (int rc, String p, Object ctx, String name, Stat stat)->{
                    Code code = Code.get(rc);
                    switch (code) {
                        case OK:
                        case NODEEXISTS:
                            log.info("signIn success. zookeeper:{} node:{}", registryProperties.getRegistryUrl(), path);
                            break;
                        case CONNECTIONLOSS:
                            signIn();
                            break;
                        default:
                            log.error("signIn fail. zookeeper:{} code:{} node:{}", registryProperties.getRegistryUrl(), code, path);
                            break;
                    }
                }, this);
    }

    private void initZookeeper() {
        if (!initZookeeper) {
            synchronized(this){
                if (!initZookeeper) {
                    try {
                        zooKeeper = new ZooKeeper(registryProperties.getRegistryUrl(), 5000, event->{
                            if (KeeperState.SyncConnected == event.getState()) {
                                this.initZookeeper = true;
                            }
                        });
                    } catch (Exception e) {
                        log.warn("initZookeeper zookeeper:{} err:{}", registryProperties.getRegistryUrl(), e.getMessage(), e);
                    }
                }
            }
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
        }
    }

    public static void main(String[] args) throws Exception {
        try (ZooKeeper zooKeeper = new ZooKeeper("localhost:2181", 2000, event -> {
            //System.out.println(event.getPath());
            //System.out.println(event.getState());
            //System.out.println(event.getType());
        })) {
            zooKeeper.addWatch("/server/hello", event->{
                System.out.println(event);
            }, AddWatchMode.PERSISTENT_RECURSIVE);
            List<String> children = zooKeeper.getChildren("/lwz", false);
            System.out.println(children);
            while (true) {
                TimeUnit.MINUTES.sleep(1);
            }
        }
    }

}

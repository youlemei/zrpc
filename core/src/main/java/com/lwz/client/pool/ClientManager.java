package com.lwz.client.pool;

import com.lwz.client.ClientProperties;
import com.lwz.client.ZrpcClient;
import com.lwz.registry.DirectRegistrar;
import com.lwz.registry.Registrar;
import com.lwz.registry.ServerInfo;
import com.lwz.registry.ZooKeeperRegistrar;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * @author liweizhou 2020/4/27
 */
@Slf4j
public class ClientManager {

    private ClientProperties clientProperties;

    private Registrar registrar;

    private CountDownLatch registrarInit;

    private volatile List<ClientPool> clientPoolList = new ArrayList<>();

    private AtomicBoolean stop = new AtomicBoolean(false);

    public ClientManager(ClientProperties clientProperties) {
        this.clientProperties = clientProperties;
        initRegistrar();
    }

    private void initRegistrar() {
        if (clientProperties.getRegistry() == null || clientProperties.getRegistry().getRegistryType() == null) {
            registrar = new DirectRegistrar(clientProperties.getNodes());
        } else {
            switch (clientProperties.getRegistry().getRegistryType()) {
                case ZOOKEEPER:
                    registrar = new ZooKeeperRegistrar(clientProperties.getRegistry());
                    break;
                default:
                    throw new IllegalArgumentException(String.format("registryType:%s not implement",
                            clientProperties.getRegistry().getRegistryType()));
            }
        }

        registrarInit = new CountDownLatch(1);
        registrar.setListener(serverInfos -> {
            //暂不考虑serverInfo重复问题
            List<ClientPool> oldClientPools = this.clientPoolList;
            List<ClientPool> newClientPools = serverInfos.stream().map(serverInfo ->
                    Optional.ofNullable(findByServerInfo(oldClientPools, serverInfo))
                            .orElse(new ClientPool(this, serverInfo, clientProperties))
            ).collect(Collectors.toList());
            List<ClientPool> toClosePool = oldClientPools.stream()
                    .filter(clientPool -> !newClientPools.contains(clientPool)).collect(Collectors.toList());
            this.clientPoolList = newClientPools;
            registrarInit.countDown();
            toClosePool.parallelStream().forEach(ClientPool::close);
        });
    }

    public ZrpcClient borrowObject() throws Exception {
        List<ClientPool> clientPools = checkForBorrow();
        //调用时间加入权重
        int index = ThreadLocalRandom.current().nextInt(clientPools.size());
        //熔断降级应该是对服务整体而言的, Fallback处理, Reject异常(配置要恰当)
        //TODO: 超时熔断
        ClientPool clientPool = clientPools.get(index);
        try {
            return clientPool.borrowObject();
        } catch (Exception e) {
            if (e instanceof IOException) {
                clientPool.disable();
            }
            throw e;
        }
    }

    private List<ClientPool> checkForBorrow() {
        try {
            registrarInit.await();
        } catch (InterruptedException e) {
            //ignore
        }
        List<ClientPool> clientPoolList = this.clientPoolList;
        //服务器升级-暂时不可用, 应采取nginx的暂时摘除策略, Socket异常销毁链接, 暂时摘除, n秒后重试
        List<ClientPool> clientPools = clientPoolList.stream().filter(ClientPool::isAvailable).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(clientPools)) {
            throw new NoSuchElementException("no available server");
        }
        return clientPools;
    }

    public void returnObject(ZrpcClient zrpcClient) {
        if (zrpcClient != null) {
            List<ClientPool> clientPools = this.clientPoolList;
            ClientPool clientPool = findByServerInfo(clientPools, zrpcClient.getServerInfo());
            if (clientPool != null) {
                clientPool.returnObject(zrpcClient);
            } else {
                //pool已被丢弃
                zrpcClient.close();
            }
        }
    }

    public void destroy() {
        if (stop.compareAndSet(false, true)) {
            List<ClientPool> clientPools = this.clientPoolList;
            clientPools.parallelStream().forEach(ClientPool::close);
            //registrar.signOut();
        }
    }

    private ClientPool findByServerInfo(List<ClientPool> oldClientPools, ServerInfo serverInfo) {
        for (int i = 0; i < oldClientPools.size(); i++) {
            ClientPool clientPool = oldClientPools.get(i);
            if (Objects.equals(clientPool.getServerInfo(), serverInfo)) {
                return clientPool;
            }
        }
        return null;
    }
}

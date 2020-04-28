package com.lwz.client.pool;

import com.lwz.client.ClientProperties;
import com.lwz.client.ZrpcClient;
import com.lwz.registry.DirectRegistrar;
import com.lwz.registry.Registrar;
import com.lwz.registry.ZooKeeperRegistrar;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * @author liweizhou 2020/4/27
 */
public class ClientManager {

    private ClientProperties clientProperties;

    private Registrar registrar;

    private volatile List<ClientPool> clientPoolList = new ArrayList<>();

    private AtomicBoolean stop = new AtomicBoolean(false);

    public ClientManager(ClientProperties clientProperties) {
        this.clientProperties = clientProperties;
        init();
    }

    private void init() {
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

        CountDownLatch registrarInit = new CountDownLatch(1);
        registrar.setListener(serverInfos -> {
            if (stop.get()) {
                return;
            }
            //暂不考虑serverInfo重复问题
            List<ClientPool> clientPools = serverInfos.stream().map(serverInfo -> {
                int index = clientPoolList.indexOf(serverInfo);
                return index > -1 ? clientPoolList.get(index) : new ClientPool(serverInfo, clientProperties);
            }).collect(Collectors.toList());
            List<ClientPool> toClosePool = this.clientPoolList.stream()
                    .filter(clientPool -> !clientPools.contains(clientPool)).collect(Collectors.toList());
            this.clientPoolList = clientPools;
            registrarInit.countDown();
            toClosePool.parallelStream().forEach(ClientPool::close);
        });

        try {
            registrarInit.await();
        } catch (InterruptedException e) {
            //ignore
        }
    }

    public ZrpcClient borrowObject() throws Exception {
        if (CollectionUtils.isEmpty(clientPoolList)) {
            throw new NoSuchElementException("no available server");
        }
        final List<ClientPool> clientPools = this.clientPoolList;
        int index = ThreadLocalRandom.current().nextInt(clientPools.size());
        //TODO: 超时熔断
        return clientPools.get(index).borrowObject();
    }

    public void returnObject(ZrpcClient zrpcClient) throws Exception {
        if (zrpcClient != null) {
            final List<ClientPool> clientPools = this.clientPoolList;
            int index = clientPools.indexOf(zrpcClient.getServerInfo());
            if (index > -1) {
                clientPools.get(index).returnObject(zrpcClient);
            } else {
                zrpcClient.close();
            }
        }
    }

    public void close() {
        if (stop.compareAndSet(false, true)) {
            final List<ClientPool> clientPools = this.clientPoolList;
            clientPools.parallelStream().forEach(ClientPool::close);
        }
    }
}

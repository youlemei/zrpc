package com.lwz.client.pool;

import com.lwz.client.ClientProperties;
import com.lwz.client.ZrpcClient;
import com.lwz.registry.DirectRegistrar;
import com.lwz.registry.Registrar;
import com.lwz.registry.ServerInfo;
import com.lwz.registry.ZooKeeperRegistrar;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
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

    private Class<?> clientInterface;

    private Registrar registrar;

    private CountDownLatch registrarInit;

    private volatile List<ClientPool> clientPoolList = new ArrayList<>();

    private AtomicBoolean stop = new AtomicBoolean(false);

    public ClientManager(ClientProperties clientProperties, Class<?> clientInterface) {
        this.clientProperties = clientProperties;
        this.clientInterface = clientInterface;
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
            if (stop.get()) {
                return;
            }
            //暂不考虑serverInfo重复问题
            List<ClientPool> oldClientPools = this.clientPoolList;
            List<ClientPool> newClientPools = serverInfos.stream()
                    .map(serverInfo -> Optional.ofNullable(findByServerInfo(oldClientPools, serverInfo))
                            .orElse(new ClientPool(this, serverInfo, clientProperties)))
                    .collect(Collectors.toList());
            this.clientPoolList = newClientPools;
            registrarInit.countDown();
            oldClientPools.stream().filter(clientPool -> !newClientPools.contains(clientPool)).parallel().forEach(ClientPool::close);
        });
    }

    public ZrpcClient borrowObject() throws Exception {
        List<ClientPool> clientPools = checkForBorrow();
        //调用时间加入权重
        int index = ThreadLocalRandom.current().nextInt(clientPools.size());
        ClientPool clientPool = clientPools.get(index);
        return clientPool.borrowObject();
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
            throw new NoSuchElementException(String.format("no available server:%s", clientInterface.getName()));
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
            registrar.signOut();
        }
    }

    private ClientPool findByServerInfo(List<ClientPool> oldClientPools, ServerInfo serverInfo) {
        for (ClientPool clientPool : oldClientPools) {
            if (Objects.equals(clientPool.getServerInfo(), serverInfo)) {
                return clientPool;
            }
        }
        return null;
    }
}

package com.lwz.client.pool;

import com.lwz.annotation.Client;
import com.lwz.client.ClientConfig;
import com.lwz.client.ZrpcClient;
import com.lwz.registry.Registrar;
import com.lwz.registry.ServerInfo;
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
 * 一个服务器一个连接池, 方便进行负载均衡/服务升级剔除/熔断降级
 *
 * @author liweizhou 2020/4/27
 */
@Slf4j
public class ClientManager {

    private ClientConfig clientConfig;

    private Class<?> clientInterface;

    private Registrar registrar;

    private CountDownLatch registrarInit;

    private volatile List<ClientPool> clientPoolList = new ArrayList<>();

    private AtomicBoolean stop = new AtomicBoolean(false);

    public ClientManager(ClientConfig clientConfig, Registrar registrar, Class<?> clientInterface) {
        this.clientConfig = clientConfig;
        this.registrar = registrar;
        this.clientInterface = clientInterface;
        initRegistrar();
    }

    private void initRegistrar() {
        registrarInit = new CountDownLatch(1);
        Client client = clientInterface.getAnnotation(Client.class);
        registrar.addListener(client.value(), serverInfos -> {
            if (stop.get()) {
                return;
            }
            //暂不考虑serverInfo重复问题
            List<ClientPool> oldClientPools = this.clientPoolList;
            List<ClientPool> newClientPools = serverInfos.stream()
                    .map(serverInfo -> Optional.ofNullable(findByServerInfo(oldClientPools, serverInfo))
                            .orElse(new ClientPool(this, serverInfo, clientConfig)))
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
            throw new NoSuchElementException(String.format("no available server: %s", clientInterface.getName()));
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

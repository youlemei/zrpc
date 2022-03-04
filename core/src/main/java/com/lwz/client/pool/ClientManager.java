package com.lwz.client.pool;

import com.lwz.annotation.Client;
import com.lwz.client.ClientConfig;
import com.lwz.client.ZrpcClient;
import com.lwz.registry.Registrar;
import com.lwz.registry.ServerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 一个服务器一个连接池, 方便进行负载均衡/服务升级剔除/熔断降级
 *
 * @author liweizhou 2020/4/27
 */
public class ClientManager {

    private static final Logger log = LoggerFactory.getLogger(ClientManager.class);

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
        addListener();
    }

    private void addListener() {
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
                            .orElse(new ClientPool(serverInfo, clientConfig)))
                    .collect(Collectors.toList());
            this.clientPoolList = newClientPools;
            registrarInit.countDown();
            oldClientPools.stream().filter(clientPool -> !newClientPools.contains(clientPool)).parallel().forEach(ClientPool::close);
        });
    }

    public ZrpcClient borrowObject() throws Exception {
        //TODO: 路线选择, 同机房->同地区->所有
        List<ClientPool> clientPools = checkClientPools();
        //调用时间加入权重
        int index = ThreadLocalRandom.current().nextInt(clientPools.size());
        ClientPool clientPool = clientPools.get(index);
        return clientPool.borrowObject();
    }

    private List<ClientPool> checkClientPools() {
        try {
            Assert.isTrue(registrarInit.await(10, TimeUnit.SECONDS));
        } catch (Exception ignore) {
        }
        List<ClientPool> clientPoolList = this.clientPoolList;
        //服务器升级-暂时不可用, 应采取nginx的暂时摘除策略, Socket异常销毁链接, 暂时摘除, n秒后重试
        List<ClientPool> clientPools = clientPoolList.stream().filter(ClientPool::isAvailable).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(clientPools)) {
            throw new NoSuchElementException(String.format("No Available Server: %s", clientInterface.getName()));
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

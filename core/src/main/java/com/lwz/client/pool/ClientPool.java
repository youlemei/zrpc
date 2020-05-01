package com.lwz.client.pool;

import com.lwz.client.ClientProperties;
import com.lwz.client.ZrpcClient;
import com.lwz.registry.ServerInfo;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.SingleThreadEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.util.concurrent.TimeUnit;

/**
 * @author liweizhou 2020/4/17
 */
@Slf4j
public class ClientPool {

    private ClientManager clientManager;

    private ServerInfo serverInfo;

    private GenericObjectPool<ZrpcClient> zrpcClientPool;

    private volatile boolean status = true;

    private SingleThreadEventExecutor renewExecutor = new DefaultEventExecutor();

    public ClientPool(ClientManager clientManager, ServerInfo serverInfo, ClientProperties clientProperties) {
        this.clientManager = clientManager;
        this.serverInfo = serverInfo;
        ClientFactory clientFactory = new ClientFactory(this, serverInfo, clientProperties.getTimeout());
        PoolProperties pool = clientProperties.getPool();
        GenericObjectPoolConfig<ZrpcClient> poolConfig = new GenericObjectPoolConfig<>();
        if (pool != null) {
            poolConfig.setMaxTotal(pool.getMaxActive());
            poolConfig.setMaxWaitMillis(pool.getMaxWait());
            poolConfig.setTestOnBorrow(pool.isTestOnBorrow());
            if (pool.isTestWhileIdle()) {
                poolConfig.setTestWhileIdle(true);
                poolConfig.setNumTestsPerEvictionRun(-1);
                poolConfig.setTimeBetweenEvictionRunsMillis(pool.getTimeBetweenEvictionRunsMillis());
            }
        }
        this.zrpcClientPool = new GenericObjectPool<>(clientFactory, poolConfig);
    }

    public ZrpcClient borrowObject() throws Exception {
        return zrpcClientPool.borrowObject();
    }

    public void invalidateObject(ZrpcClient zrpcClient) {
        try {
            zrpcClientPool.invalidateObject(zrpcClient);
        } catch (Exception e) {
            log.warn("invalidateObject fail. err:{}", e.getMessage(), e);
        }
    }

    public void returnObject(ZrpcClient zrpcClient) {
        zrpcClientPool.returnObject(zrpcClient);
    }

    public void close() {
        try {
            zrpcClientPool.close();
            log.info("pool close success. {}:{}", serverInfo.getHost(), serverInfo.getPort());
        } catch (Exception e) {
            log.warn("pool close fail. {}:{} err:{}", serverInfo.getHost(), serverInfo.getPort(), e.getMessage());
        } finally {
            renewExecutor.shutdownGracefully();
        }
    }

    public ServerInfo getServerInfo() {
        return serverInfo;
    }

    public boolean isAvailable() {
        return status;
    }

    public void disable() {
        status = false;
        addRenewTask();
    }

    private void addRenewTask() {
        renewExecutor.schedule(() -> {
            try {
                ZrpcClient zrpcClient = zrpcClientPool.borrowObject();
                if (zrpcClient != null) {
                    status = true;
                    return;
                }
            } catch (Exception e) {
                log.warn("renew fail. err:{}", e.getMessage());
            }
            addRenewTask();
        }, 10, TimeUnit.SECONDS);
    }

}

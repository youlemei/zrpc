package com.lwz.client.pool;

import com.lwz.client.ClientProperties;
import com.lwz.client.ZrpcClient;
import com.lwz.registry.ServerInfo;
import org.apache.commons.pool2.impl.GenericObjectPool;

import java.io.Closeable;

/**
 * @author liweizhou 2020/4/17
 */
public class ClientPool implements Closeable {

    private ServerInfo serverInfo;

    private GenericObjectPool<ZrpcClient> zrpcClientPool;

    public ClientPool(ServerInfo serverInfo, ClientProperties clientProperties) {
        this.serverInfo = serverInfo;
        ClientFactory clientFactory = new ClientFactory(serverInfo, clientProperties.getTimeout());
        this.zrpcClientPool = new GenericObjectPool<>(clientFactory, clientProperties.getPool());
    }

    public ZrpcClient borrowObject() throws Exception {
        return zrpcClientPool.borrowObject();
    }

    public void invalidateObject(ZrpcClient zrpcClient) throws Exception {
        if (zrpcClient != null) {
            zrpcClientPool.invalidateObject(zrpcClient);
        }
    }

    public void returnObject(ZrpcClient zrpcClient) throws Exception {
        if (zrpcClient != null) {
            zrpcClientPool.returnObject(zrpcClient);
        }
    }

    @Override
    public void close() {
        zrpcClientPool.close();
    }

    public ServerInfo getServerInfo() {
        return serverInfo;
    }
}

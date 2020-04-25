package com.lwz.client.pool;

import com.lwz.client.ClientProperties;
import com.lwz.client.ZrpcClient;
import org.apache.commons.pool2.impl.GenericObjectPool;

import java.io.Closeable;

/**
 * @author liweizhou 2020/4/17
 */
public class ClientPool implements Closeable {

    private GenericObjectPool<ZrpcClient> zrpcClientPool;

    public ClientPool(ClientProperties clientProperties) {
        ClientFactory clientFactory = new ClientFactory(clientProperties);
        zrpcClientPool = new GenericObjectPool<>(clientFactory, clientProperties.getPool());
    }

    public ZrpcClient borrowObject() throws Exception {
        return zrpcClientPool.borrowObject();
    }

    public void invalidateObject(ZrpcClient obj) throws Exception {
        if (obj != null) {
            zrpcClientPool.invalidateObject(obj);
        }
    }

    public void returnObject(ZrpcClient obj) throws Exception {
        if (obj != null) {
            zrpcClientPool.returnObject(obj);
        }
    }

    @Override
    public void close() {
        //TODO: 等待10秒, 然后逐个关闭
        zrpcClientPool.close();
    }
}

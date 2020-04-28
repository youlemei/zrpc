package com.lwz.client.pool;

import com.lwz.client.ZrpcClient;
import com.lwz.registry.ServerInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

/**
 * @author liweizhou 2020/4/17
 */
@Slf4j
public class ClientFactory implements PooledObjectFactory<ZrpcClient> {

    private ServerInfo serverInfo;

    private int timeout;

    public ClientFactory(ServerInfo serverInfo, int timeout) {
        this.serverInfo = serverInfo;
        this.timeout = timeout;
    }

    @Override
    public PooledObject<ZrpcClient> makeObject() throws Exception {
        ZrpcClient zrpcClient = new ZrpcClient(serverInfo, timeout);
        return new DefaultPooledObject<>(zrpcClient);
    }

    @Override
    public void destroyObject(PooledObject<ZrpcClient> p) throws Exception {
        ZrpcClient zrpcClient = p.getObject();
        if (zrpcClient != null) {
            zrpcClient.close();
        }
    }

    @Override
    public boolean validateObject(PooledObject<ZrpcClient> p) {
        ZrpcClient zrpcClient = p.getObject();
        if (zrpcClient == null) {
            return false;
        }
        try {
            zrpcClient.ping();
            return true;
        } catch (Exception e) {
            log.warn("ping fail. {} err:{}", zrpcClient.getServerInfo(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void activateObject(PooledObject<ZrpcClient> p) throws Exception {
    }

    @Override
    public void passivateObject(PooledObject<ZrpcClient> p) throws Exception {
    }
}

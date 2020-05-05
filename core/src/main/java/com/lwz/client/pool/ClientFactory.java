package com.lwz.client.pool;

import com.lwz.client.ZrpcClient;
import com.lwz.registry.ServerInfo;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * @author liweizhou 2020/4/17
 */
public class ClientFactory implements PooledObjectFactory<ZrpcClient> {

    private static final Logger log = LoggerFactory.getLogger(ClientFactory.class);

    private ClientPool clientPool;

    private ServerInfo serverInfo;

    private int timeout;

    public ClientFactory(ClientPool clientPool, ServerInfo serverInfo, int timeout) {
        this.clientPool = clientPool;
        this.serverInfo = serverInfo;
        this.timeout = timeout;
    }

    @Override
    public PooledObject<ZrpcClient> makeObject() throws Exception {
        ZrpcClient zrpcClient = new ZrpcClient(clientPool, serverInfo, timeout);
        return new DefaultPooledObject<>(zrpcClient);
    }

    @Override
    public void destroyObject(PooledObject<ZrpcClient> p) throws Exception {
        ZrpcClient zrpcClient = p.getObject();
        zrpcClient.close();
    }

    @Override
    public boolean validateObject(PooledObject<ZrpcClient> p) {
        ZrpcClient zrpcClient = p.getObject();
        try {
            zrpcClient.ping();
            return true;
        } catch (Exception e) {
            log.warn("ping fail. {} err:{}", zrpcClient.getServerInfo(), e.getMessage());
            return false;
        }
    }

    @Override
    public void activateObject(PooledObject<ZrpcClient> p) throws Exception {
        Assert.isTrue(p.getObject().isOpen(), "channel is inactive");
    }

    @Override
    public void passivateObject(PooledObject<ZrpcClient> p) throws Exception {
    }
}

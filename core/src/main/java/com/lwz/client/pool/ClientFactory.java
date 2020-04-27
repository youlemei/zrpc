package com.lwz.client.pool;

import com.lwz.client.ClientProperties;
import com.lwz.client.ZrpcClient;
import com.lwz.registry.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author liweizhou 2020/4/17
 */
@Slf4j
public class ClientFactory implements PooledObjectFactory<ZrpcClient> {

    private ClientProperties clientProperties;

    //TODO: 提到上一层
    private Registrar registrar;

    public ClientFactory(ClientProperties clientProperties) {
        this.clientProperties = clientProperties;
        if (clientProperties.getRegistry() == null) {
            registrar = new DirectRegistrar(clientProperties.getNodes());
        } else {
            if (RegistryType.ZOOKEEPER == clientProperties.getRegistry().getRegistryType()) {
                registrar = new ZooKeeperRegistrar(clientProperties.getRegistry());
            }
        }
    }

    @Override
    public PooledObject<ZrpcClient> makeObject() throws Exception {
        //no available server
        List<ServerInfo> serverInfos = registrar.getServerInfos();
        ServerInfo serverInfo = serverInfos.get(ThreadLocalRandom.current().nextInt(serverInfos.size()));
        ZrpcClient zrpcClient = new ZrpcClient(serverInfo, clientProperties.getTimeout());
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

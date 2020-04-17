package com.lwz.client.pool;

import com.lwz.client.ZrpcClient;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;

/**
 * @author liweizhou 2020/4/17
 */
public class ClientFactory implements PooledObjectFactory<ZrpcClient> {

    //观察者

    @Override
    public PooledObject<ZrpcClient> makeObject() throws Exception {
        //拿到host/port后,创建channel

        return null;
    }

    @Override
    public void destroyObject(PooledObject<ZrpcClient> p) throws Exception {

    }

    @Override
    public boolean validateObject(PooledObject<ZrpcClient> p) {
        return false;
    }

    @Override
    public void activateObject(PooledObject<ZrpcClient> p) throws Exception {

    }

    @Override
    public void passivateObject(PooledObject<ZrpcClient> p) throws Exception {

    }
}

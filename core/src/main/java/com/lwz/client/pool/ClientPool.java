package com.lwz.client.pool;

import com.lwz.client.ZrpcClient;
import org.apache.commons.pool2.impl.GenericObjectPool;

import java.io.Closeable;
import java.util.NoSuchElementException;

/**
 * @author liweizhou 2020/4/17
 */
public class ClientPool implements Closeable {

    private GenericObjectPool<ZrpcClient> clientPool;

    public ZrpcClient borrowObject() throws Exception, NoSuchElementException, IllegalStateException {
        return null;
    }

    public void invalidateObject(ZrpcClient obj) throws Exception {

    }

    public void returnObject(ZrpcClient obj) throws Exception {

    }

    @Override
    public void close() {
        //shutdown hook
    }
}

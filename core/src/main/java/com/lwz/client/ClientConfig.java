package com.lwz.client;

import com.lwz.client.pool.PoolProperties;

/**
 * @author liweizhou 2020/5/3
 */
public class ClientConfig {

    public static final int DEFAULT_TIMEOUT = 10;

    /**
     * 请求超时(秒)
     */
    private int timeout = DEFAULT_TIMEOUT;

    private PoolProperties pool;

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public PoolProperties getPool() {
        return pool;
    }

    public void setPool(PoolProperties pool) {
        this.pool = pool;
    }
}

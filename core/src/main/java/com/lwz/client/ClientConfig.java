package com.lwz.client;

import com.lwz.client.pool.PoolProperties;
import lombok.Data;

/**
 * @author liweizhou 2020/5/3
 */
@Data
public class ClientConfig {

    public static final int DEFAULT_TIMEOUT = 10;

    /**
     * 请求超时(秒)
     */
    private int timeout = DEFAULT_TIMEOUT;

    private PoolProperties pool;

}

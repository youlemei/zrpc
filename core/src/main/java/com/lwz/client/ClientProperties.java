package com.lwz.client;

import com.lwz.client.pool.PoolProperties;
import com.lwz.registry.RegistryProperties;
import lombok.Data;

import java.util.List;

/**
 * @author liweizhou 2020/4/12
 */
@Data
public class ClientProperties {

    public static final int DEFAULT_TIMEOUT = 10;

    /**
     * 直连节点
     */
    private List<String> nodes;

    /**
     * 请求超时(秒)
     */
    private int timeout = DEFAULT_TIMEOUT;

    private RegistryProperties registry;

    private PoolProperties pool;

}

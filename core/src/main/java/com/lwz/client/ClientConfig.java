package com.lwz.client;

import com.lwz.registry.RegistryType;
import lombok.Data;

/**
 * @author liweizhou 2020/4/12
 */
@Data
public class ClientConfig {

    private String host;

    private int port;

    private int timeout;

    /**
     * 注册中心类型
     */
    private RegistryType registryType = RegistryType.DEFAULT;

}

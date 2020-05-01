package com.lwz.registry;

import lombok.Data;

/**
 * @author liweizhou 2020/4/25
 */
@Data
public class RegistryProperties {

    public static final String DEFAULT_ROOT_PATH = "/server";

    /**
     * 注册中心地址
     */
    private String registryUrl;
    /**
     * 服务名, 必填
     */
    private String serverName;

    /**
     * 服务密钥, 注册使用
     */
    //private String serverKey;

    /**
     * 注册中心类型
     */
    private RegistryType registryType = RegistryType.ZOOKEEPER;

    private String rootPath = DEFAULT_ROOT_PATH;

}

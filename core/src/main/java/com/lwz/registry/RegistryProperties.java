package com.lwz.registry;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author liweizhou 2020/4/25
 */
@Data
@ConfigurationProperties(prefix = "zrpc.registry")
public class RegistryProperties {

    public static final String DEFAULT_ROOT_PATH = "/server";

    /**
     * 注册中心地址
     */
    private String registryUrl;

    /**
     * 注册中心类型
     */
    private RegistryType registryType = RegistryType.DIRECT;

    private String rootPath = DEFAULT_ROOT_PATH;

    private Map<String, List<String>> direct = new HashMap<>();

}

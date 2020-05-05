package com.lwz.registry;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author liweizhou 2020/4/25
 */
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

    public enum RegistryType {

        DIRECT, ZOOKEEPER

    }

    public String getRegistryUrl() {
        return registryUrl;
    }

    public void setRegistryUrl(String registryUrl) {
        this.registryUrl = registryUrl;
    }

    public RegistryType getRegistryType() {
        return registryType;
    }

    public void setRegistryType(RegistryType registryType) {
        this.registryType = registryType;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public Map<String, List<String>> getDirect() {
        return direct;
    }

    public void setDirect(Map<String, List<String>> direct) {
        this.direct = direct;
    }
}

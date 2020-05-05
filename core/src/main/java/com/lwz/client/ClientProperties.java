package com.lwz.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @author liweizhou 2020/4/12
 */
@ConfigurationProperties(prefix = "zrpc.client")
public class ClientProperties {

    private Map<String, ClientConfig> config = new HashMap<>();

    public Map<String, ClientConfig> getConfig() {
        return config;
    }

    public void setConfig(Map<String, ClientConfig> config) {
        this.config = config;
    }
}

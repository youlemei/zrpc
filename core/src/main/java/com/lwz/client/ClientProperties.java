package com.lwz.client;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @author liweizhou 2020/4/12
 */
@Data
@ConfigurationProperties(prefix = "zrpc.client")
public class ClientProperties {

    private Map<String, ClientConfig> config = new HashMap<>();

}

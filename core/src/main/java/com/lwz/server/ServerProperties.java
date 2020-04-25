package com.lwz.server;

import com.lwz.registry.RegistryProperties;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author liweizhou 2020/4/5
 */
@Data
@ConfigurationProperties("zrpc.server")
public class ServerProperties {

    /**
     * 服务端口
     */
    private int port;

    /**
     * SO_TIMEOUT
     */
    private int timeout;

    private RegistryProperties registry;

}

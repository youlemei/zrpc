package com.lwz.server;

import com.lwz.registry.RegistryType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author liweizhou 2020/4/5
 */
@Data
@ConfigurationProperties("zrpc.server")
public class ServerConfig {

    /**
     * 服务端口
     */
    private int port;

    /**
     * SO_TIMEOUT
     */
    private int timeout;

    /**
     * 服务名, 必填
     */
    private String serverName;

    /**
     * 服务密钥, 注册使用
     */
    private String serverKey;

    /**
     * 注册中心类型
     */
    private RegistryType registryType = RegistryType.DEFAULT;

}

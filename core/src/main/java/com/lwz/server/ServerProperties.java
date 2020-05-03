package com.lwz.server;

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
    private int port = 7320;

    /**
     * 默认超时
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

}

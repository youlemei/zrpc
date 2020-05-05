package com.lwz.server;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author liweizhou 2020/4/5
 */
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

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getServerKey() {
        return serverKey;
    }

    public void setServerKey(String serverKey) {
        this.serverKey = serverKey;
    }
}

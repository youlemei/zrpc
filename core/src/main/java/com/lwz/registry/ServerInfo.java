package com.lwz.registry;

import lombok.Data;

/**
 * @author liweizhou 2020/4/17
 */
@Data
public class ServerInfo {

    private String host;

    private int port;

    public ServerInfo(String host, int port) {
        this.host = host;
        this.port = port;
    }

}

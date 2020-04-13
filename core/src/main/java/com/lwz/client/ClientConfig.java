package com.lwz.client;

import lombok.Data;

/**
 * @author liweizhou 2020/4/12
 */
@Data
public class ClientConfig {

    private String host;

    private int port;

    private int timeout;

}

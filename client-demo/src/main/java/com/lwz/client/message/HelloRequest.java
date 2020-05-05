package com.lwz.client.message;

import com.alibaba.fastjson.JSON;
import com.lwz.annotation.Field;
import com.lwz.annotation.Message;

/**
 * @author liweizhou 2020/4/12
 */
@Message
public class HelloRequest {

    @Field(1)
    private String host;

    @Field(2)
    private int port;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}

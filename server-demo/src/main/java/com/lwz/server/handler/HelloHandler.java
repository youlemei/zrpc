package com.lwz.server.handler;

import com.lwz.annotation.Handler;
import com.lwz.annotation.Server;
import com.lwz.server.message.HelloRequest;
import com.lwz.server.message.HelloResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author liweizhou 2020/4/12
 */
@Slf4j
@Server
public class HelloHandler {

    @Handler(1)
    public HelloResponse hello(HelloRequest helloRequest) {

        log.info("hello {}", helloRequest);

        HelloResponse helloResponse = new HelloResponse();
        helloResponse.setTime(System.currentTimeMillis());
        return helloResponse;
    }

    @Handler(2)
    public int hello2(HelloRequest helloRequest, List<Long> list, int ret) {

        log.info("hello2 {} {} {}", helloRequest, list, ret);

        return ret;
    }

}

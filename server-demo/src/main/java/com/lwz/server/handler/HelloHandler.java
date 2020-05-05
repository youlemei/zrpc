package com.lwz.server.handler;

import com.lwz.annotation.Handler;
import com.lwz.annotation.Server;
import com.lwz.server.message.HelloRequest;
import com.lwz.server.message.HelloResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * @author liweizhou 2020/4/12
 */
@Server
public class HelloHandler {

    private static final Logger log = LoggerFactory.getLogger(HelloHandler.class);

    @Handler(1)
    public HelloResponse hello(HelloRequest helloRequest) throws Exception {

        log.info("hello {}", helloRequest);

        TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextLong(1000));

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

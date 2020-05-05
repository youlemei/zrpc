package com.lwz.client.client;

import com.lwz.client.ResponseFuture;
import com.lwz.client.message.HelloRequest;
import com.lwz.client.message.HelloResponse;
import com.lwz.client.pool.ClientFallback;

import java.util.List;

/**
 * @author liweizhou 2020/5/1
 */
//@Component
public class HelloClientFallback implements HelloClient, ClientFallback {
    @Override
    public HelloResponse hello(HelloRequest helloRequest) {
        return new HelloResponse();
    }

    @Override
    public ResponseFuture<HelloResponse> helloAsync(HelloRequest helloRequest) {
        return null;
    }

    @Override
    public void helloOnly(HelloRequest helloRequest) {

    }

    @Override
    public int hello2(HelloRequest helloRequest, List<Long> list, int ret) {
        return 0;
    }
}

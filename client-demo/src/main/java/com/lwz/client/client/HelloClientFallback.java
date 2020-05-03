package com.lwz.client.client;

import com.lwz.client.message.HelloRequest;
import com.lwz.client.message.HelloResponse;
import com.lwz.client.pool.ClientFallback;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

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
    public Future<HelloResponse> helloAsync(HelloRequest helloRequest) {
        return CompletableFuture.completedFuture(new HelloResponse());
    }

    @Override
    public void helloOnly(HelloRequest helloRequest) {

    }

    @Override
    public int hello2(HelloRequest helloRequest, List<Long> list, int ret) {
        return 0;
    }
}

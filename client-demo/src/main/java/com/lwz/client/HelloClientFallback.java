package com.lwz.client;

import com.lwz.client.message.HelloRequest;
import com.lwz.client.message.HelloResponse;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * @author liweizhou 2020/5/1
 */
@Component
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
}

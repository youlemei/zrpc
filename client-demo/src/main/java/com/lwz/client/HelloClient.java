package com.lwz.client;

import com.lwz.annotation.Client;
import com.lwz.annotation.Request;
import com.lwz.client.message.HelloRequest;
import com.lwz.client.message.HelloResponse;

import java.util.concurrent.Future;

/**
 * @author liweizhou 2020/4/13
 */
@Client("hello")
public interface HelloClient {

    @Request(1)
    HelloResponse hello(HelloRequest helloRequest);

    @Request(1)
    Future<HelloResponse> helloAsync(HelloRequest helloRequest);

    @Request(1)
    void helloOnly(HelloRequest helloRequest);

}

package com.lwz.client;

import com.lwz.annotation.Client;
import com.lwz.annotation.Request;
import com.lwz.client.message.HelloRequest;
import com.lwz.client.message.HelloResponse;

/**
 * @author liweizhou 2020/4/13
 */
@Client("hello")
public interface HelloClient {

    @Request(1)
    HelloResponse hello(HelloRequest helloRequest);

}

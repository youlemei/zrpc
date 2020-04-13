package com.lwz;

import com.lwz.annotation.ClientScan;
import com.lwz.client.HelloClient;
import com.lwz.client.message.HelloRequest;
import com.lwz.client.message.HelloResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author liweizhou 2020/4/13
 */
@ClientScan("com.lwz.client")
@SpringBootApplication
public class Main implements ApplicationRunner {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Main.class, args);
    }

    @Autowired
    private HelloClient helloClient;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        HelloRequest helloRequest = new HelloRequest();
        helloRequest.setName("lwz");
        helloRequest.setAge(123);
        HelloResponse resp = helloClient.hello(helloRequest);
    }
}

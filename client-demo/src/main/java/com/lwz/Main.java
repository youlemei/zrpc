package com.lwz;

import com.lwz.annotation.ClientScan;
import com.lwz.client.HelloClient;
import com.lwz.client.message.HelloRequest;
import com.lwz.client.message.HelloResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.Future;

/**
 * @author liweizhou 2020/4/13
 */
@Slf4j
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
        helloRequest.setHost("lwz");
        helloRequest.setPort(7320);

        HelloResponse resp1 = helloClient.hello(helloRequest);
        HelloResponse resp2 = helloClient.hello(helloRequest);
        HelloResponse resp3 = helloClient.hello(helloRequest);
        HelloResponse resp4 = helloClient.hello(helloRequest);
        HelloResponse resp5 = helloClient.hello(helloRequest);

        log.info("resp:{}", resp1);
        log.info("resp:{}", resp2);
        log.info("resp:{}", resp3);
        log.info("resp:{}", resp4);
        log.info("resp:{}", resp5);

        Future<HelloResponse> future1 = helloClient.helloAsync(helloRequest);
        Future<HelloResponse> future2 = helloClient.helloAsync(helloRequest);
        Future<HelloResponse> future3 = helloClient.helloAsync(helloRequest);
        Future<HelloResponse> future4 = helloClient.helloAsync(helloRequest);
        Future<HelloResponse> future5 = helloClient.helloAsync(helloRequest);

        log.info("resp:{}", future1.get());
        log.info("resp:{}", future2.get());
        log.info("resp:{}", future3.get());
        log.info("resp:{}", future4.get());
        log.info("resp:{}", future5.get());

        helloClient.helloOnly(helloRequest);
        helloClient.helloOnly(helloRequest);
        helloClient.helloOnly(helloRequest);
        helloClient.helloOnly(helloRequest);
        helloClient.helloOnly(helloRequest);

        log.info("hello end.");
    }
}

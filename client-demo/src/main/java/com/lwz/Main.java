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
        helloRequest.setName("lwz");
        helloRequest.setAge(123);
        HelloResponse resp = helloClient.hello(helloRequest);
        log.info("resp:{}", resp);
        log.info("resp:{}", helloClient.hello(helloRequest));
        log.info("resp:{}", helloClient.hello(helloRequest));
        log.info("resp:{}", helloClient.hello(helloRequest));
        log.info("resp:{}", helloClient.hello(helloRequest));
        log.info("resp:{}", helloClient.hello(helloRequest));
        log.info("resp:{}", helloClient.hello(helloRequest));
        log.info("resp:{}", helloClient.hello(helloRequest));
        log.info("resp:{}", helloClient.hello(helloRequest));
        log.info("resp:{}", helloClient.hello(helloRequest));

        Future<HelloResponse> helloResponseFuture1 = helloClient.helloAsync(helloRequest);
        Future<HelloResponse> helloResponseFuture2 = helloClient.helloAsync(helloRequest);
        Future<HelloResponse> helloResponseFuture3 = helloClient.helloAsync(helloRequest);
        Future<HelloResponse> helloResponseFuture4 = helloClient.helloAsync(helloRequest);
        Future<HelloResponse> helloResponseFuture5 = helloClient.helloAsync(helloRequest);

        log.info("resp:{}", helloResponseFuture1.get());
        log.info("resp:{}", helloResponseFuture2.get());
        log.info("resp:{}", helloResponseFuture3.get());
        log.info("resp:{}", helloResponseFuture4.get());
        log.info("resp:{}", helloResponseFuture5.get());
    }
}

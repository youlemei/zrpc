package com.lwz.client.client;

import com.lwz.client.ResponseFuture;
import com.lwz.client.message.HelloRequest;
import com.lwz.client.message.HelloResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author liweizhou 2020/4/28
 */
@Slf4j
@Component
public class HelloClientTest {

    public static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(10);

    @Autowired
    private HelloClient helloClient;

    @Scheduled(fixedDelay = 10000)
    public void run() throws Exception {
        hello();

        //hello2();
    }

    private void hello2() {
        HelloRequest helloRequest = new HelloRequest();
        helloRequest.setHost("lwz");
        helloRequest.setPort(7777);

        int ret = helloClient.hello2(helloRequest, Arrays.asList(1L, 2L, 3L), 2);
        log.info("ret:{}", ret);
    }

    private void hello() throws Exception {
        HelloRequest helloRequest = new HelloRequest();
        helloRequest.setHost("lwz");
        helloRequest.setPort(7320);

        for (int i = 0; i < 10; i++) {
            EXECUTOR_SERVICE.execute(()-> log.info("hello:{}", helloClient.hello(helloRequest)));
        }

        for (int i = 0; i < 10; i++) {
            ResponseFuture<HelloResponse> future = helloClient.helloAsync(helloRequest);
            future.onSuccess(helloResponse -> log.info("helloAsync:{}", helloResponse))
                    .onFail(throwable -> log.warn("helloAsync {}", throwable.getMessage()));
        }

        //helloClient.helloOnly(helloRequest);

        log.info("hello end.");
    }

}

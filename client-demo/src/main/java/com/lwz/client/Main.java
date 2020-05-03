package com.lwz.client;

import com.lwz.annotation.ClientScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author liweizhou 2020/4/13
 */
@EnableScheduling
@ClientScan("com.lwz.client.client")
@SpringBootApplication
public class Main {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Main.class, args);
    }

}

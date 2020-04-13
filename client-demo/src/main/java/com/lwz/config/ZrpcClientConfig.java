package com.lwz.config;

import com.lwz.client.ClientConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author liweizhou 2020/4/13
 */
@Configuration
public class ZrpcClientConfig {

    @Bean
    @ConfigurationProperties("zrpc.client.hello")
    public ClientConfig hello(){
        return new ClientConfig();
    }

}

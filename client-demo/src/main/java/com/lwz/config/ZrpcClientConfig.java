package com.lwz.config;

import com.lwz.client.ClientProperties;
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
    public ClientProperties hello(){
        return new ClientProperties();
    }

}

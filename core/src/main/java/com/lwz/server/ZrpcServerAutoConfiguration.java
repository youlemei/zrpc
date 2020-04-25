package com.lwz.server;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * @author liweizhou 2020/4/5
 */
@Import(HandlerRegistrar.class)
@ConditionalOnProperty(value = "zrpc.server.enabled")
@EnableConfigurationProperties(ServerProperties.class)
public class ZrpcServerAutoConfiguration {

    @Bean
    public DispatcherHandler dispatcherHandler(HandlerRegistrar handlerRegistrar){
        return new DispatcherHandler(handlerRegistrar);
    }

    @Bean
    public ZrpcServer zrpcServer(ServerProperties serverProperties, DispatcherHandler dispatcherHandler){
        return new ZrpcServer(serverProperties, dispatcherHandler);
    }

}

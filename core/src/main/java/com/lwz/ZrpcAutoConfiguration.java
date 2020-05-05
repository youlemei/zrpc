package com.lwz;

import com.lwz.client.ClientProperties;
import com.lwz.registry.DirectRegistrar;
import com.lwz.registry.Registrar;
import com.lwz.registry.RegistryProperties;
import com.lwz.registry.ZooKeeperRegistrar;
import com.lwz.server.HandlerRegistrar;
import com.lwz.server.ServerProperties;
import com.lwz.server.ZrpcServer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * @author liweizhou 2020/4/5
 */
@Import(HandlerRegistrar.class)
@ConditionalOnProperty(value = "zrpc.server.enabled", matchIfMissing = true)
@EnableConfigurationProperties({ServerProperties.class, ClientProperties.class, RegistryProperties.class})
public class ZrpcAutoConfiguration {

    @Bean
    public Registrar registrar(RegistryProperties registryProperties) {
        switch (registryProperties.getRegistryType()) {
            case DIRECT:
                return new DirectRegistrar(registryProperties.getDirect());
            case ZOOKEEPER:
                return new ZooKeeperRegistrar(registryProperties);
            default:
                throw new IllegalArgumentException(String.format("registryType:%s not implement", registryProperties.getRegistryType()));
        }
    }

    @Bean
    public ZrpcServer zrpcServer(ServerProperties serverProperties, HandlerRegistrar handlerRegistrar, Registrar registrar){
        return new ZrpcServer(serverProperties, handlerRegistrar, registrar);
    }

}

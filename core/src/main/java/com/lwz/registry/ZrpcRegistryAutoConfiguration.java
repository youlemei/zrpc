package com.lwz.registry;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * @author liweizhou 2020/5/3
 */
@EnableConfigurationProperties(RegistryProperties.class)
public class ZrpcRegistryAutoConfiguration {

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

}

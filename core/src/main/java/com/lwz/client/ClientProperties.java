package com.lwz.client;

import com.lwz.registry.RegistryProperties;
import lombok.Data;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.util.List;

/**
 * @author liweizhou 2020/4/12
 */
@Data
public class ClientProperties {

    private List<String> nodes;

    private int timeout;

    private RegistryProperties registry;

    private GenericObjectPoolConfig<ZrpcClient> pool;

}

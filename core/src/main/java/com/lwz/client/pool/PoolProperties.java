package com.lwz.client.pool;

import lombok.Data;
import org.apache.commons.pool2.impl.BaseObjectPoolConfig;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/**
 * @author liweizhou 2020/5/1
 */
@Data
public class PoolProperties {

    private int maxActive = GenericObjectPoolConfig.DEFAULT_MAX_TOTAL;

    private long maxWait = BaseObjectPoolConfig.DEFAULT_MAX_WAIT_MILLIS;

    private boolean testOnBorrow = true;

    private boolean testWhileIdle = true;

    private long timeBetweenEvictionRunsMillis = 60000;

}

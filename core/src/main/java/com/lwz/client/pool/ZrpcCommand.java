package com.lwz.client.pool;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixThreadPoolKey;

/**
 * @author liweizhou 2020/4/29
 */
public class ZrpcCommand<R> extends HystrixCommand {

    public ZrpcCommand(String serverName, String request) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(serverName))
                .andCommandKey(HystrixCommandKey.Factory.asKey(request))
                .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(serverName + ":" + request)));

    }

    @Override
    protected Object run() throws Exception {
        return null;
    }

    @Override
    protected Object getFallback() {
        return null;
    }

}

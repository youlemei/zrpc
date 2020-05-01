package com.lwz.client.pool;

import com.lwz.client.ClientFallback;
import com.lwz.client.FallbackContext;
import com.lwz.client.FallbackException;
import com.lwz.client.MethodMetadata;
import com.lwz.client.ResponseFuture;
import com.lwz.client.ZrpcClient;
import com.lwz.message.ZZPHeader;
import com.lwz.message.ZZPMessage;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixEventType;
import com.netflix.hystrix.HystrixThreadPoolKey;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeoutException;

/**
 * @author liweizhou 2020/4/29
 */
public class ZrpcCommand extends HystrixCommand {

    private MethodMetadata methodMetadata;

    private ClientManager clientManager;

    private Object clientFallback;

    private Object[] args;

    public ZrpcCommand(MethodMetadata methodMetadata, ClientManager clientManager, Object clientFallback, Object[] args) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(methodMetadata.getServerKey()))
                .andCommandKey(HystrixCommandKey.Factory.asKey(methodMetadata.getMethodKey()))
                .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(methodMetadata.getMethodKey()))
                //TODO: 全局默认配置/独立线程池配置
                .andCommandPropertiesDefaults(HystrixCommandProperties.defaultSetter().withExecutionTimeoutEnabled(false))
        );
        this.methodMetadata = methodMetadata;
        this.clientManager = clientManager;
        this.clientFallback = clientFallback;
        this.args = args;
    }

    @Override
    protected Object run() throws Exception {
        ZrpcClient zrpcClient = null;
        ResponseFuture responseFuture;
        try {
            ZZPMessage message = new ZZPMessage();
            ZZPHeader header = new ZZPHeader();
            header.setUri(methodMetadata.getRequest().value());
            message.setHeader(header);
            if (methodMetadata.getArgsIndex() >= 0) {
                message.setBody(args[methodMetadata.getArgsIndex()]);
            }
            zrpcClient = clientManager.borrowObject();
            responseFuture = zrpcClient.request(message, methodMetadata.getReturnType());
            //异常处理
        } finally {
            clientManager.returnObject(zrpcClient);
        }
        if (methodMetadata.isAsync()) {
            //异步失败
            //com.netflix.hystrix.HystrixCommandMetrics.HealthCounts.plus
            responseFuture.onFail(t -> {
                if (t instanceof TimeoutException) {
                    eventNotifier.markEvent(HystrixEventType.TIMEOUT, commandKey);
                } else {
                    eventNotifier.markEvent(HystrixEventType.FAILURE, commandKey);
                }
            });
            return responseFuture;
        }
        return responseFuture.get();
    }

    @Override
    protected Object getFallback() {
        try {
            //未实现, 抛run异常 实现了, 抛fallback异常
            if (clientFallback == null) {
                throw FallbackException.FALLBACK_NOT_FOUND;
            }
            Throwable executionException = getExecutionException();
            FallbackContext fallbackContext = new FallbackContext();
            fallbackContext.setCause(executionException);
            ClientFallback.FALLBACK_THREAD_LOCAL.set(fallbackContext);
            return methodMetadata.getMethod().invoke(clientFallback, args);
        } catch (Throwable e) {
            if (e instanceof InvocationTargetException) {
                e = ((InvocationTargetException) e).getTargetException();
            }
            throw new FallbackException(e);
        } finally {
            ClientFallback.FALLBACK_THREAD_LOCAL.remove();
        }
    }

}

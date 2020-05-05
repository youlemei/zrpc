package com.lwz.client.pool;

import com.lwz.client.MethodMetadata;
import com.lwz.client.ResponseFuture;
import com.lwz.client.ZrpcClient;
import com.lwz.message.EncodeObj;
import com.lwz.message.Header;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixEventType;
import com.netflix.hystrix.HystrixThreadPoolKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeoutException;

/**
 * @author liweizhou 2020/4/29
 */
public class RequestCommand extends HystrixCommand {

    private static final Logger log = LoggerFactory.getLogger(RequestCommand.class);

    private MethodMetadata methodMetadata;

    private ClientManager clientManager;

    private Object clientFallback;

    private Object[] args;

    public RequestCommand(MethodMetadata methodMetadata, ClientManager clientManager, Object clientFallback, Object[] args) {
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
            EncodeObj encodeObj = new EncodeObj();
            Header header = new Header();
            header.setUri(methodMetadata.getRequest().value());
            encodeObj.setHeader(header);
            encodeObj.setBodys(args);
            encodeObj.setBodyTypes(methodMetadata.getArgsTypes());
            zrpcClient = clientManager.borrowObject();
            responseFuture = zrpcClient.request(encodeObj, methodMetadata.getReturnType());
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
        //未实现, 抛run异常 实现了, 抛fallback异常
        try {
            Throwable executionException = getExecutionException();
            if (clientFallback == null) {
                throw executionException;
            }
            FallbackContext fallbackContext = new FallbackContext();
            fallbackContext.setCause(executionException);
            ClientFallback.FALLBACK_THREAD_LOCAL.set(fallbackContext);
            log.info("invoke fallback {}", methodMetadata.getMethodKey());
            return methodMetadata.getMethod().invoke(clientFallback, args);
        } catch (Throwable e) {
            if (e instanceof InvocationTargetException) {
                e = ((InvocationTargetException) e).getTargetException();
            }
            throw new RuntimeException(e);
        } finally {
            ClientFallback.FALLBACK_THREAD_LOCAL.remove();
        }
    }

}

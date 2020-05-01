package com.lwz.client;

import com.lwz.annotation.Request;
import com.lwz.client.pool.ClientManager;
import com.lwz.client.pool.ZrpcCommand;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author liweizhou 2020/4/13
 */
@Data
@Slf4j
public class RequestInvoker implements InvocationHandler {

    private ClientManager clientManager;

    private Object clientFallback;

    private ConcurrentMap<Method, MethodMetadata> metadataMap = new ConcurrentHashMap<>();

    public RequestInvoker(Class<?> clientInterface, ClientManager clientManager, Object clientFallback) {
        this.clientManager = clientManager;
        this.clientFallback = clientFallback;
        init(clientInterface);
    }

    private void init(Class<?> clientInterface) {
        Method[] methods = ReflectionUtils.getDeclaredMethods(clientInterface);
        for (Method method : methods) {
            Request request = method.getAnnotation(Request.class);
            Assert.notNull(request, "method must annotation with @Request");
            metadataMap.put(method, new MethodMetadata(request, method));
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("toString")) {
            return toString();
        }
        if (method.getName().equals("equals")) {
            return equals(args[0]);
        }
        if (method.getName().equals("hashcode")) {
            return hashCode();
        }
        MethodMetadata methodMetadata = metadataMap.get(method);
        if (methodMetadata == null) {
            return null;
        }
        try {
            //熔断降级应该是对服务整体而言的
            ZrpcCommand zrpcCommand = new ZrpcCommand(methodMetadata, clientManager, clientFallback, args);
            return zrpcCommand.execute();
        } catch (Throwable e) {
            if (e instanceof HystrixRuntimeException) {
                HystrixRuntimeException hystrix = (HystrixRuntimeException) e;
                Throwable fallbackException = hystrix.getFallbackException();
                if (fallbackException instanceof FallbackException) {
                    if (fallbackException == FallbackException.FALLBACK_NOT_FOUND) {
                        throw hystrix.getCause();
                    }
                    throw fallbackException.getCause();
                }
                throw hystrix.getCause();
            }
            throw e;
        }
    }

}

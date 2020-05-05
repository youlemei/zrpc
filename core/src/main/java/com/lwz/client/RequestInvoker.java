package com.lwz.client;

import com.lwz.annotation.Request;
import com.lwz.client.pool.ClientManager;
import com.lwz.client.pool.RequestCommand;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

/**
 * @author liweizhou 2020/4/13
 */
public class RequestInvoker implements InvocationHandler {

    private static final Logger log = LoggerFactory.getLogger(RequestInvoker.class);

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
            RequestCommand requestCommand = new RequestCommand(methodMetadata, clientManager, clientFallback, args);
            return requestCommand.execute();
        } catch (Throwable e) {
            if (e instanceof HystrixRuntimeException) {
                HystrixRuntimeException hystrix = (HystrixRuntimeException) e;
                e = hystrix.getFallbackException().getCause();
                if (e instanceof ExecutionException) {
                    e = e.getCause();
                }
            }
            //代理, new Exception可以让异常栈更好看
            throw new RequestException(e);
        }
    }

}

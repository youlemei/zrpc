package com.lwz.client;

import com.lwz.annotation.Request;
import com.lwz.client.pool.ClientManager;
import com.lwz.client.pool.ZrpcCommand;
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

    private ConcurrentMap<Method, MethodMetadata> metadataMap = new ConcurrentHashMap<>();

    public RequestInvoker(Class<?> clientInterface, ClientManager clientManager) {
        this.clientManager = clientManager;
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
            ZrpcCommand zrpcCommand = new ZrpcCommand(methodMetadata, clientManager, args);
            return zrpcCommand.execute();
        } catch (Exception e) {
            log.warn("invoke fail. err:{} type:{}", e.getMessage(), e.getClass().getName());
            throw e.getCause();
            //return null;
        }
    }

}

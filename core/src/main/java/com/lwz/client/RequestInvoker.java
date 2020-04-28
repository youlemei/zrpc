package com.lwz.client;

import com.lwz.annotation.Message;
import com.lwz.annotation.Request;
import com.lwz.client.pool.ClientManager;
import com.lwz.message.ZZPHeader;
import com.lwz.message.ZZPMessage;
import lombok.Data;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

/**
 * @author liweizhou 2020/4/13
 */
@Data
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
            int index = -1;
            Class<?>[] parameterTypes = method.getParameterTypes();
            for (int i = 0; i < parameterTypes.length; i++) {
                if (parameterTypes[i].getAnnotation(Message.class) != null) {
                    index = i;
                    break;
                }
            }
            Class<?> returnType = method.getReturnType();
            if (!returnType.equals(void.class) && !Future.class.isAssignableFrom(returnType)) {
                Assert.notNull(returnType.getAnnotation(Message.class), "method return type must annotation with @Message");
            }
            metadataMap.put(method, new MethodMetadata(request, index));
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
        ZrpcClient zrpcClient = null;
        ResponseFuture responseFuture;
        Class<?> returnType = method.getReturnType();
        Class<?> actualType = returnType;
        if (Future.class.isAssignableFrom(returnType)) {
            ParameterizedType genericReturnType = (ParameterizedType) method.getGenericReturnType();
            //genericReturnType.getRawType() is Future.class
            actualType = (Class<?>) genericReturnType.getActualTypeArguments()[0];
        }
        try {
            ZZPMessage message = new ZZPMessage();
            ZZPHeader header = new ZZPHeader();
            header.setUri(methodMetadata.getRequest().value());
            message.setHeader(header);
            if (methodMetadata.getArgsIndex() >= 0) {
                message.setBody(args[methodMetadata.getArgsIndex()]);
            }
            zrpcClient = clientManager.borrowObject();
            responseFuture = zrpcClient.request(message, actualType);
        } finally {
            clientManager.returnObject(zrpcClient);
        }
        if (returnType.equals(void.class)) {
            return null;
        }
        if (Future.class.isAssignableFrom(returnType)) {
            return responseFuture;
        }
        //TODO: 默认超时设置 结合熔断
        return responseFuture.get();
    }

}

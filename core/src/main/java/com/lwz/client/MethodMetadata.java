package com.lwz.client;

import com.lwz.annotation.Message;
import com.lwz.annotation.Request;
import lombok.Data;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * @author liweizhou 2020/4/25
 */
@Data
public class MethodMetadata {

    private Request request;

    private Method method;

    private int argsIndex;

    private boolean async;

    private String serverKey;

    private String methodKey;

    private Class<?> returnType;

    public MethodMetadata(Request request, Method method) {
        this.request = request;
        this.method = method;
        init();
    }

    private void init() {
        int index = -1;
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (parameterTypes[i].getAnnotation(Message.class) != null) {
                index = i;
                break;
            }
        }
        argsIndex = index;
        serverKey = method.getDeclaringClass().getName();
        methodKey = serverKey + "#" + method.getName() +
                Arrays.stream(parameterTypes).map(Class::getName).collect(Collectors.joining(",", "(", ")"));

        Class<?> returnType = method.getReturnType();
        if (returnType.equals(void.class)) {
            //async = true; //void时同步or异步?
            return;
        }
        if (Future.class.isAssignableFrom(returnType)) {
            async = true;
            ParameterizedType genericReturnType = (ParameterizedType) method.getGenericReturnType();
            returnType = (Class<?>) genericReturnType.getActualTypeArguments()[0];
        }
        Assert.notNull(returnType.getAnnotation(Message.class), "method return type must annotation with @Message");
        this.returnType = returnType;
    }

}

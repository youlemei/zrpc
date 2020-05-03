package com.lwz.client;

import com.lwz.annotation.Request;
import com.lwz.codec.Codecs;
import lombok.Data;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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

    private Type[] argsTypes;

    private boolean async;

    private String serverKey;

    private String methodKey;

    private Type returnType;

    public MethodMetadata(Request request, Method method) {
        this.request = request;
        this.method = method;
        init();
    }

    private void init() {
        argsTypes = method.getGenericParameterTypes();
        serverKey = method.getDeclaringClass().getName();
        methodKey = serverKey + "#" + method.getName() +
                Arrays.stream(argsTypes).map(Type::getTypeName).collect(Collectors.joining(",", "(", ")"));

        Type genericReturnType = method.getGenericReturnType();
        if (genericReturnType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
            if (Future.class.isAssignableFrom((Class<?>) parameterizedType.getRawType())) {
                async = true;
                genericReturnType = parameterizedType.getActualTypeArguments()[0];
            }
        }
        returnType = genericReturnType;
        //check
        try {
            for (Type type : argsTypes) {
                Codecs.length(type, null);
            }
            Codecs.length(returnType, null);
        } catch (Exception e) {
            throw new IllegalArgumentException("func check fail.", e);
        }
    }

    public Type[] getArgsTypes() {
        return argsTypes;
    }
}

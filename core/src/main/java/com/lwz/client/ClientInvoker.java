package com.lwz.client;

import com.lwz.annotation.Message;
import com.lwz.annotation.Request;
import com.lwz.codec.Messager;
import com.lwz.message.ZZPHeader;
import com.lwz.message.ZZPMessage;
import io.netty.buffer.ByteBuf;
import lombok.Data;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author liweizhou 2020/4/13
 */
@Data
public class ClientInvoker implements InvocationHandler {

    private Class<?> clientInterface;

    private ClientProperties clientConfig;

    private ZrpcClient zrpcClient;

    public ClientInvoker(Class<?> clientInterface, ClientProperties clientConfig) {
        this.clientInterface = clientInterface;
        this.clientConfig = clientConfig;
        //TODO: 池化
        //TODO: shudownhook
        this.zrpcClient = new ZrpcClient(clientConfig);
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
        for (Method clientInterfaceMethod : clientInterface.getMethods()) {
            if (clientInterfaceMethod.equals(method)) {
                Request request = method.getAnnotation(Request.class);
                int uri = request.value();
                Object req = null;
                Class<?>[] parameterTypes = method.getParameterTypes();
                for (int i = 0; i < parameterTypes.length; i++) {
                    if (parameterTypes[i].getAnnotation(Message.class) != null) {
                        req = args[i];
                        break;
                    }
                }

                ZZPMessage message = new ZZPMessage();
                ZZPHeader header = new ZZPHeader();
                header.setUri(uri);
                message.setHeader(header);
                message.setBody(req);
                ResponseFuture responseFuture = zrpcClient.request(message);
                ByteBuf respBuf = (ByteBuf) responseFuture.get();
                Class<?> returnType = method.getReturnType();
                //TODO: Future
                if (returnType.getAnnotation(Message.class) != null) {
                    return Messager.read(respBuf, returnType);
                }
                return null;
            }
        }
        return null;
    }

}

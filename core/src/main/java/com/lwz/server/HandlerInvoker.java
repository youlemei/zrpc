package com.lwz.server;

import com.lwz.annotation.Message;
import com.lwz.codec.Messager;
import com.lwz.filter.Filter;
import com.lwz.message.ZZPHeader;
import com.lwz.message.ZZPMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * @author liweizhou 2020/4/5
 */
public class HandlerInvoker {

    private int uri;

    private Object bean;

    private Method method;

    private List<Filter> filters = Collections.emptyList();

    public HandlerInvoker(int uri, Object bean, Method method) {
        this.uri = uri;
        this.bean = bean;
        this.method = method;
    }

    public boolean applyPreHandle(ChannelHandlerContext ctx, ZZPMessage msg) {
        for (Filter filter : filters) {
            if (!filter.preHandle(ctx, msg)) {
                return false;
            }
        }
        return true;
    }

    public void applyPostHandle(ChannelHandlerContext ctx, ZZPMessage msg) {
        for (Filter filter : filters) {
            filter.postHandle(ctx, msg);
        }
    }

    public void handle(ChannelHandlerContext ctx, ZZPMessage msg) throws Exception {
        //拼参, 调用, 返回
        //TODO: 接口化 decode encode handle
        Object[] args = getMethodArgs(msg);
        Object result = method.invoke(bean, args);
        ZZPMessage message = new ZZPMessage();
        message.setHeader(msg.getHeader());
        message.setBody(result);
        ctx.channel().writeAndFlush(message);
    }

    private Object[] getMethodArgs(ZZPMessage msg) {
        ByteBuf bodyBuf = (ByteBuf) msg.getBody();
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] args = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            if (parameterType.equals(ZZPHeader.class)) {
                args[i] = msg.getHeader();
                continue;
            }
            if (parameterType.getAnnotation(Message.class) != null) {
                Object arg = Messager.read(bodyBuf, parameterType);
                args[i] = arg;
                break;
            }
        }
        return args;
    }

    public Object getBean() {
        return bean;
    }

    public Method getMethod() {
        return method;
    }

}

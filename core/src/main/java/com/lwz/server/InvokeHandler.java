package com.lwz.server;

import com.lwz.filter.Filter;
import com.lwz.protocol.ZZPHeader;
import com.lwz.protocol.ZZPMessage;
import io.netty.channel.ChannelHandlerContext;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * @author liweizhou 2020/4/5
 */
public class InvokeHandler {

    private Object bean;

    private Method method;

    private List<Filter> filters = Collections.emptyList();

    public InvokeHandler(Object bean, Method method) {
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
        Object[] args = getMethodArgs(msg);
        Object result = method.invoke(bean, args);
        ZZPMessage message = new ZZPMessage();
        message.setHeader(msg.getHeader());
        message.setBody(result);
        ctx.channel().writeAndFlush(message);
    }

    private Object[] getMethodArgs(ZZPMessage msg) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] args = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            if (parameterType.equals(ZZPHeader.class)) {
                args[i] = msg.getHeader();
                continue;
            }
            if (msg.getBody() != null && parameterType.equals(msg.getBody().getClass())) {
                args[i] = msg.getBody();
                continue;
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

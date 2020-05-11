package com.lwz.server;

import com.lwz.codec.ZrpcCodecs;
import com.lwz.filter.Filter;
import com.lwz.message.DecodeObj;
import com.lwz.message.EncodeObj;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

/**
 * @author liweizhou 2020/4/5
 */
public class HandlerInvoker {

    private int uri;

    private Object bean;

    private Method method;

    private Type[] paramTypes;

    private Type[] returnTypes;

    private List<Filter> filters = Collections.emptyList();

    public HandlerInvoker(int uri, Object bean, Method method) {
        this.uri = uri;
        this.bean = bean;
        this.method = method;
        init();
    }

    private void init() {
        paramTypes = method.getGenericParameterTypes();
        returnTypes = new Type[]{method.getGenericReturnType()};
        //check
        try {
            for (Type type : paramTypes) {
                ZrpcCodecs.length(type, null);
            }
            for (Type type : returnTypes) {
                ZrpcCodecs.length(type, null);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("func check fail.", e);
        }
    }

    public boolean applyPreHandle(ChannelHandlerContext ctx, DecodeObj msg) {
        for (Filter filter : filters) {
            if (!filter.preHandle(ctx, msg)) {
                return false;
            }
        }
        return true;
    }

    public void applyPostHandle(ChannelHandlerContext ctx, DecodeObj msg) {
        for (Filter filter : filters) {
            filter.postHandle(ctx, msg);
        }
    }

    public void handle(ChannelHandlerContext ctx, DecodeObj msg) throws Exception {
        //拼参, 调用, 返回
        Object[] args = getMethodArgs(msg);
        Object result = method.invoke(bean, args);
        EncodeObj encodeObj = new EncodeObj();
        encodeObj.setHeader(msg.getHeader());
        encodeObj.setBodys(new Object[]{result});
        encodeObj.setBodyTypes(returnTypes);
        ctx.channel().writeAndFlush(encodeObj);
    }

    private Object[] getMethodArgs(DecodeObj msg) throws Exception {
        ByteBuf bodyBuf = msg.getBody();
        Object[] args = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            Object arg = ZrpcCodecs.read(bodyBuf, paramTypes[i]);
            args[i] = arg;
        }
        //if (bodyBuf.readableBytes() != 0) {
        //    throw new DecoderException("byteBuf size is bigger");
        //}
        return args;
    }

    public Object getBean() {
        return bean;
    }

    public Method getMethod() {
        return method;
    }

}

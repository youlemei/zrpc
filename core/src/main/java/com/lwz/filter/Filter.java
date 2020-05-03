package com.lwz.filter;

import com.lwz.message.DecodeObj;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author liweizhou 2020/4/5
 */
public interface Filter {

    /**
     * 前置处理器, 拦截成功时, 自定义响应
     *
     * @param ctx
     * @param msg
     * @return
     */
    boolean preHandle(ChannelHandlerContext ctx, DecodeObj msg);

    /**
     * 后置处理器
     *
     * @param ctx
     * @param msg
     */
    void postHandle(ChannelHandlerContext ctx, DecodeObj msg);

}

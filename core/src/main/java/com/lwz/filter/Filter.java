package com.lwz.filter;

import com.lwz.message.ZrpcDecodeObj;
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
    boolean preHandle(ChannelHandlerContext ctx, ZrpcDecodeObj msg);

    /**
     * 后置处理器, 根据前置处理器倒序处理
     *
     * @param ctx
     * @param msg
     */
    void postHandle(ChannelHandlerContext ctx, ZrpcDecodeObj msg);

}

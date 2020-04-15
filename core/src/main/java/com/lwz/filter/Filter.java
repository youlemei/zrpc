package com.lwz.filter;

import com.lwz.message.ZZPMessage;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author liweizhou 2020/4/5
 */
public interface Filter {

    boolean preHandle(ChannelHandlerContext ctx, ZZPMessage msg);

    void postHandle(ChannelHandlerContext ctx, ZZPMessage msg);

}

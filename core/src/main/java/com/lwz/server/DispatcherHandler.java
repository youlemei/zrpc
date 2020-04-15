package com.lwz.server;

import com.lwz.message.ZZPMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @author liweizhou 2020/4/5
 */
@ChannelHandler.Sharable
public class DispatcherHandler extends SimpleChannelInboundHandler<ZZPMessage> {

    private HandlerRegistrar handlerRegistrar;

    public DispatcherHandler(HandlerRegistrar handlerRegistrar) {
        this.handlerRegistrar = handlerRegistrar;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ZZPMessage msg) throws Exception {
        InvokeHandler handler = handlerRegistrar.findHandler(msg.getHeader().getUri());
        if (handler == null) {
            //err404

        }

        try {
            if (!handler.applyPreHandle(ctx, msg)) {
                return;
            }

            //exception
            handler.handle(ctx, msg);

            handler.applyPostHandle(ctx, msg);

        } catch (Throwable e) {
            e.printStackTrace();
        } finally {

            //ThreadLocal.remove
        }

    }

    //TODO: 处理异常响应

}

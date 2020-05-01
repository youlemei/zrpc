package com.lwz.server;

import com.lwz.message.ZZPMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * @author liweizhou 2020/4/5
 */
@Slf4j
@ChannelHandler.Sharable
public class DispatcherHandler extends SimpleChannelInboundHandler<ZZPMessage> {

    private HandlerRegistrar handlerRegistrar;

    public DispatcherHandler(HandlerRegistrar handlerRegistrar) {
        this.handlerRegistrar = handlerRegistrar;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ZZPMessage msg) throws Exception {
        HandlerInvoker handler = handlerRegistrar.findHandler(msg.getHeader().getUri());
        if (handler == null) {
            //err404

        }

        //TODO: 调用链 ThreadLocal

        try {
            if (!handler.applyPreHandle(ctx, msg)) {
                return;
            }

            //exception
            handler.handle(ctx, msg);

            handler.applyPostHandle(ctx, msg);

        } catch (Throwable e) {
            //responseErr();
            log.error("channelRead0 fail. header:{} err:{}", msg.getHeader(), e.getMessage(), e);

        } finally {

            //ThreadLocal.remove
            ReferenceCountUtil.release(msg.getBody());
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 异常处理
        log.error("server channel err:{} type:{}", cause.getMessage(), cause.getClass().getName(), cause);
        if (cause instanceof IOException) {
            ctx.channel().close();
        }
    }

}

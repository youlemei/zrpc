package com.lwz.server;

import com.lwz.message.DecodeObj;
import com.lwz.message.EncodeObj;
import com.lwz.message.ErrMessage;
import com.lwz.message.Header;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.concurrent.ForkJoinPool;

/**
 * @author liweizhou 2020/4/5
 */
@Slf4j
@ChannelHandler.Sharable
public class DispatcherHandler extends SimpleChannelInboundHandler<DecodeObj> {

    private HandlerRegistrar handlerRegistrar;

    public DispatcherHandler(HandlerRegistrar handlerRegistrar) {
        this.handlerRegistrar = handlerRegistrar;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DecodeObj msg) throws Exception {

        //TODO: 调用链 请求信息(ip/port/header) ThreadLocal

        ForkJoinPool.commonPool().execute(()->{
            try {
                HandlerInvoker handler = handlerRegistrar.findHandler(msg.getHeader().getUri());
                if (handler == null) {
                    //err404
                    throw new HandlerException("404. Handler Not Found");
                }
                if (!handler.applyPreHandle(ctx, msg)) {
                    return;
                }

                //TODO: async ???
                handler.handle(ctx, msg);

                handler.applyPostHandle(ctx, msg);

            } catch (Throwable e) {
                //responseErr();
                log.warn("handle fail. header:{} err:{}", msg.getHeader(), e.getMessage(), e);
                responseErr(ctx, msg, e);

            } finally {

                //ThreadLocal.remove
                ReferenceCountUtil.release(msg.getBody());
            }
        });


    }

    private void responseErr(ChannelHandlerContext ctx, DecodeObj msg, Throwable e) {

        ErrMessage errMessage = new ErrMessage();
        errMessage.setMessage(e.getMessage());
        errMessage.setException(e.getClass().getName());

        //处理器
        if (e instanceof DecoderException) {
            errMessage.setMessage("400. Bad Request");
        }
        if (e instanceof EncoderException) {
            errMessage.setMessage("500. Server Error");
        }

        EncodeObj encodeObj = new EncodeObj();
        Header header = msg.getHeader();
        header.setExt(Header.EXCEPTION);
        encodeObj.setHeader(header);
        encodeObj.setBodys(new Object[]{errMessage});
        encodeObj.setBodyTypes(new Type[]{ErrMessage.class});
        ctx.channel().writeAndFlush(encodeObj);
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

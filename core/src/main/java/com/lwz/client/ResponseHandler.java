package com.lwz.client;

import com.lwz.codec.ZrpcCodecs;
import com.lwz.message.DecodeObj;
import com.lwz.message.ErrMessage;
import com.lwz.message.Header;
import com.lwz.server.HandlerException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author liweizhou 2020/4/12
 */
public class ResponseHandler extends SimpleChannelInboundHandler<DecodeObj> {

    private static final Logger log = LoggerFactory.getLogger(ResponseHandler.class);

    private ZrpcClient zrpcClient;

    public ResponseHandler(ZrpcClient zrpcClient) {
        this.zrpcClient = zrpcClient;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DecodeObj msg) throws Exception {
        try {
            Header header = msg.getHeader();
            ResponseFutureImpl responseFuture = zrpcClient.getResponseFuture(header.getSeq());
            if (responseFuture != null) {
                if (header.isException()) {
                    ErrMessage err = ZrpcCodecs.read(msg.getBody(), ErrMessage.class);
                    responseFuture.fail(new HandlerException(err.getMessage()));
                } else {
                    Object resp = ZrpcCodecs.read(msg.getBody(), responseFuture.getReturnType());
                    responseFuture.success(resp);
                }
            } else {
                log.info("response discard header:{}", header);
            }
        } finally {
            ReferenceCountUtil.release(msg.getBody());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 异常处理
        log.error("client channel err:{} type:{}", cause.getMessage(), cause.getClass().getName(), cause);
        if (cause instanceof IOException) {
            //关闭连接池, 或使连接池不可用
            zrpcClient.disablePool();
        }
    }
}

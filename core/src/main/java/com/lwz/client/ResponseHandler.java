package com.lwz.client;

import com.lwz.message.ZZPMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * @author liweizhou 2020/4/12
 */
@Slf4j
public class ResponseHandler extends SimpleChannelInboundHandler<ZZPMessage> {

    private ZrpcClient zrpcClient;

    public ResponseHandler(ZrpcClient zrpcClient) {
        this.zrpcClient = zrpcClient;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ZZPMessage msg) throws Exception {
        ResponseFutureImpl responseFuture = zrpcClient.getResponseFuture(msg.getHeader().getSeq());
        if (responseFuture != null) {
            responseFuture.success(msg.getBody());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //TODO: 异常处理
        log.error("client channel err:{} type:{}", cause.getMessage(), cause.getClass().getName(), cause);
        if (cause instanceof IOException) {
            //关闭连接池, 或使连接池不可用
            zrpcClient.disablePool();
        }
    }
}

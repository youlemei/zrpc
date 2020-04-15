package com.lwz.client;

import com.lwz.message.ZZPMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @author liweizhou 2020/4/12
 */
public class ResponseHandler extends SimpleChannelInboundHandler<ZZPMessage> {

    private ZrpcClient zrpcClient;

    public ResponseHandler(ZrpcClient zrpcClient) {
        this.zrpcClient = zrpcClient;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ZZPMessage msg) throws Exception {
        ResponseFutureImpl responseFuture = zrpcClient.getResponseFuture(msg.getHeader().getSeq());
        if (responseFuture != null) {
            responseFuture.complete(msg.getBody());
        }
    }
    
}

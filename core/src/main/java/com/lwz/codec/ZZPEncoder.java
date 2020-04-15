package com.lwz.codec;

import com.lwz.message.ZZPMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author liweizhou 2020/4/5
 */
public class ZZPEncoder extends MessageToByteEncoder<ZZPMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ZZPMessage msg, ByteBuf out) throws Exception {
        //zzpMessage -> byteBuf
        int length = Messager.getLength(msg.getBody());
        msg.getHeader().setLength(length);
        Messager.write(out, msg.getHeader());
        Messager.write(out, msg.getBody());
    }

    //TODO: 打包异常响应

}

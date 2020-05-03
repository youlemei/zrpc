package com.lwz.codec;

import com.lwz.message.Header;
import com.lwz.message.EncodeObj;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.lang.reflect.Type;

/**
 * @author liweizhou 2020/4/5
 */
public class ZrpcEncoder extends MessageToByteEncoder<EncodeObj> {

    @Override
    protected void encode(ChannelHandlerContext ctx, EncodeObj msg, ByteBuf out) throws Exception {
        int length = 0;
        Type[] bodyTypes = msg.getBodyTypes();
        if (bodyTypes != null) {
            for (int i = 0; i < bodyTypes.length; i++) {
                length += ZrpcCodecs.length(bodyTypes[i], msg.getBodys()[i]);
            }
        }
        msg.getHeader().setLength(length);
        ZrpcCodecs.write(out, Header.class, msg.getHeader());
        if (bodyTypes != null) {
            for (int i = 0; i < bodyTypes.length; i++) {
                ZrpcCodecs.write(out, bodyTypes[i], msg.getBodys()[i]);
            }
        }
    }

}

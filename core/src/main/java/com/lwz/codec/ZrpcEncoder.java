package com.lwz.codec;

import com.lwz.message.Header;
import com.lwz.message.ZrpcEncodeObj;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.lang.reflect.Type;

/**
 * @author liweizhou 2020/4/5
 */
public class ZrpcEncoder extends MessageToByteEncoder<ZrpcEncodeObj> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ZrpcEncodeObj msg, ByteBuf out) throws Exception {
        int length = 0;
        Type[] bodyTypes = msg.getBodyTypes();
        if (bodyTypes != null) {
            for (int i = 0; i < bodyTypes.length; i++) {
                length += Codecs.length(bodyTypes[i], msg.getBodys()[i]);
            }
        }
        msg.getHeader().setLength(length);
        Codecs.write(out, Header.class, msg.getHeader());
        if (bodyTypes != null) {
            for (int i = 0; i < bodyTypes.length; i++) {
                Codecs.write(out, bodyTypes[i], msg.getBodys()[i]);
            }
        }
    }

}

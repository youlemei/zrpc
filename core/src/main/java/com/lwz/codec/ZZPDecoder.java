package com.lwz.codec;

import com.lwz.message.ZZPHeader;
import com.lwz.message.ZZPMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author liweizhou 2020/4/5
 */
@Slf4j
public class ZZPDecoder extends ByteToMessageDecoder {

    public static final int MAX_BYTEBUF_LENGTH = 30 * 1024 * 1024;

    public static final int MAX_MESSAGE_LENGTH = 10 * 1024 * 1024;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        //byteBuf -> zzpMessage
        if (in.readableBytes() > MAX_BYTEBUF_LENGTH) {
            //堆积请求过多, 主动中断
            log.warn("decode err. readable byteBuf length > {}, close channel.", MAX_BYTEBUF_LENGTH);
            ctx.channel().close();
        }

        while (true) {
            if (in.readableBytes() < ZZPHeader.HEADER_LENGTH) {
                //继续等待数据包
                return;
            }

            //TODO: 结束

            ByteBuf headerBuf = in.slice(in.readerIndex(), in.readerIndex() + ZZPHeader.HEADER_LENGTH);
            ZZPHeader header = Messager.read(headerBuf, ZZPHeader.class);
            int length = header.getLength();
            if (length > MAX_MESSAGE_LENGTH) {
                log.warn("decode err. body length > {}, close channel.", MAX_MESSAGE_LENGTH);
                ctx.channel().close();
                return;
            }
            if (header.getVersion() != Messager.VERSION) {
                log.warn("decode err. version expect:{} but:{}, close channel.", Messager.VERSION, header.getVersion());
                ctx.channel().close();
                return;
            }
            int messageLength = ZZPHeader.HEADER_LENGTH + length;
            if (in.readableBytes() < messageLength) {
                //继续等待数据包
                return;
            }

            ByteBuf messageBuf = in.readBytes(messageLength);
            messageBuf.readerIndex(ZZPHeader.HEADER_LENGTH);

            if (isPing(header.getExt())) {
                header.setExt(ZZPHeader.PONG);
                ZZPMessage message = new ZZPMessage();
                message.setHeader(header);
                ctx.channel().writeAndFlush(message);
                return;
            }

            ZZPMessage message = new ZZPMessage();
            message.setHeader(header);
            message.setBody(messageBuf);
            out.add(message);
        }

    }

    private boolean isPing(short ext) {
        return (ext & ZZPHeader.PING) > 0;
    }

    private boolean isPong(short ext) {
        return (ext & ZZPHeader.PONG) > 0;
    }

    //TODO: 解包异常响应

}

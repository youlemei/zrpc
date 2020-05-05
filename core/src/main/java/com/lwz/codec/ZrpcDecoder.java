package com.lwz.codec;

import com.lwz.message.DecodeObj;
import com.lwz.message.EncodeObj;
import com.lwz.message.Header;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author liweizhou 2020/4/5
 */
public class ZrpcDecoder extends ByteToMessageDecoder {

    private static final Logger log = LoggerFactory.getLogger(ZrpcDecoder.class);

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
            if (in.readableBytes() < Header.HEADER_LENGTH) {
                //继续等待数据包
                return;
            }

            ByteBuf headerBuf = in.slice(in.readerIndex(), Header.HEADER_LENGTH);
            Header header = ZrpcCodecs.read(headerBuf, Header.class);
            int length = header.getLength();
            if (length > MAX_MESSAGE_LENGTH) {
                log.warn("decode err. body length > {}, close channel.", MAX_MESSAGE_LENGTH);
                ctx.channel().close();
                return;
            }
            if (header.getVersion() != ZrpcCodecs.VERSION) {
                log.warn("decode err. version expect:{} but:{}, close channel.", ZrpcCodecs.VERSION, header.getVersion());
                ctx.channel().close();
                return;
            }
            int messageLength = Header.HEADER_LENGTH + length;
            if (in.readableBytes() < messageLength) {
                //继续等待数据包
                return;
            }

            ByteBuf messageBuf = in.readBytes(messageLength);
            messageBuf.readerIndex(Header.HEADER_LENGTH);

            if (header.isPing()) {
                header.setExt(Header.PONG);
                EncodeObj encodeObj = new EncodeObj();
                encodeObj.setHeader(header);
                ctx.channel().writeAndFlush(encodeObj);
                return;
            }

            DecodeObj message = new DecodeObj();
            message.setHeader(header);
            message.setBody(messageBuf);
            out.add(message);
        }

    }

}

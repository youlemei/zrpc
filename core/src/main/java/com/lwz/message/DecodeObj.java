package com.lwz.message;

import io.netty.buffer.ByteBuf;

/**
 * @author liweizhou 2020/5/2
 */
public class DecodeObj {

    private Header header;

    private ByteBuf body;

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public ByteBuf getBody() {
        return body;
    }

    public void setBody(ByteBuf body) {
        this.body = body;
    }

}

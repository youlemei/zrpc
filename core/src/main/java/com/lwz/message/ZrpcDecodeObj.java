package com.lwz.message;

import io.netty.buffer.ByteBuf;
import lombok.Data;

/**
 * @author liweizhou 2020/5/2
 */
@Data
public class ZrpcDecodeObj {

    private Header header;

    private ByteBuf body;

}

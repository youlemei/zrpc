package com.lwz.message;

import lombok.Data;

/**
 * @author liweizhou 2020/4/5
 */
@Data
public class ZZPMessage {

    private ZZPHeader header;

    /**
     * TODO: REQ/RESP 目前是ByteBuf/Message
     */
    private Object body;

}

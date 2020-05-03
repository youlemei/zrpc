package com.lwz.message;

import com.lwz.annotation.Field;
import com.lwz.annotation.Message;
import com.lwz.codec.Codecs;
import lombok.Data;

/**
 * @author liweizhou 2020/3/29
 */
@Data
@Message
public class Header {

    /**
     * 请求uri
     */
    @Field(1)
    private int uri;

    /**
     * 请求序列号
     */
    @Field(2)
    private int seq;

    /**
     * 数据体长度(字节)
     */
    @Field(3)
    private int length;

    /**
     * 版本
     */
    @Field(4)
    private short version = Codecs.VERSION;

    /**
     * 拓展: ping/pong/exception/json
     */
    @Field(5)
    private short ext;

    public static final int HEADER_LENGTH = 16;

    public static final short PING = 1;

    public static final short PONG = 2;

    public static final short EXCEPTION = 4;

    public static final short JSON = 8;

    public boolean isPing() {
        return (ext & Header.PING) > 0;
    }

    public boolean isPong() {
        return (ext & Header.PONG) > 0;
    }

    public boolean isException() {
        return (ext & Header.EXCEPTION) > 0;
    }

    public boolean isJson() {
        return (ext & Header.JSON) > 0;
    }

}

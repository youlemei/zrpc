package com.lwz.protocol;

import com.lwz.annotation.Field;
import com.lwz.annotation.Message;
import com.lwz.codec.Messager;
import lombok.Data;

/**
 * @author liweizhou 2020/3/29
 */
@Data
@Message
public class ZZPHeader {

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
    private short version = Messager.VERSION;

    /**
     * 拓展: ping/pong/exception
     */
    @Field(5)
    private short ext;

    public static final int HEADER_LENGTH = 16;

}

package com.lwz.message;

import com.lwz.annotation.Field;
import com.lwz.annotation.Message;
import com.lwz.codec.ZrpcCodecs;

/**
 * @author liweizhou 2020/3/29
 */
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
    private short version = ZrpcCodecs.VERSION;

    /**
     * 拓展: ping/pong/exception/json
     * TODO: callType: call, oneWay, reply
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

    public int getUri() {
        return uri;
    }

    public void setUri(int uri) {
        this.uri = uri;
    }

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public short getVersion() {
        return version;
    }

    public void setVersion(short version) {
        this.version = version;
    }

    public short getExt() {
        return ext;
    }

    public void setExt(short ext) {
        this.ext = ext;
    }
}

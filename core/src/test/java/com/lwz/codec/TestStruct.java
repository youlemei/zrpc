package com.lwz.codec;

import com.lwz.annotation.Field;
import com.lwz.annotation.Message;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author liweizhou 2020/5/2
 */
@Data
@Message
public class TestStruct<T, R> {

    public TestStruct() {
    }

    public TestStruct(R r, T t, List<R> list, Map<T, R> map, TestMessage<T, String, R> testMessage) {
        this.r = r;
        this.t = t;
        this.list = list;
        this.map = map;
        this.testMessage = testMessage;
    }

    @Field(1)
    R r;

    @Field(2)
    T t;

    @Field(3)
    List<R> list;

    @Field(4)
    Map<T, R> map;

    @Field(5)
    TestMessage<T, String, R> testMessage;

}

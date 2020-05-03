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
public class TestMessage<U,V,W> {

    @Field(1)
    private List<V> list;

    @Field(2)
    private U t;

    @Field(3)
    private Map<V, W> map;
}

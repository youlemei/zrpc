package com.lwz.codec;

import com.lwz.annotation.Field;
import com.lwz.annotation.Message;

import java.util.List;
import java.util.Map;

/**
 * @author liweizhou 2020/5/2
 */
@Message
public class TestMessage<U,V,W> {

    @Field(1)
    private List<V> list;

    @Field(2)
    private U t;

    @Field(3)
    private Map<V, W> map;

    public List<V> getList() {
        return list;
    }

    public void setList(List<V> list) {
        this.list = list;
    }

    public U getT() {
        return t;
    }

    public void setT(U t) {
        this.t = t;
    }

    public Map<V, W> getMap() {
        return map;
    }

    public void setMap(Map<V, W> map) {
        this.map = map;
    }
}

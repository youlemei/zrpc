package com.lwz.client.message;

import com.alibaba.fastjson.JSON;
import com.lwz.annotation.Field;
import com.lwz.annotation.Message;

/**
 * @author liweizhou 2020/4/13
 */
@Message
public class HelloResponse {

    @Field(1)
    private long time;

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}

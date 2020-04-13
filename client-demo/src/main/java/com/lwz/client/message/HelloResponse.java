package com.lwz.client.message;

import com.lwz.annotation.Field;
import com.lwz.annotation.Message;
import lombok.Data;

/**
 * @author liweizhou 2020/4/13
 */
@Data
@Message
public class HelloResponse {

    @Field(1)
    private long time;

}

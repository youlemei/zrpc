package com.lwz.client.message;

import com.lwz.annotation.Field;
import com.lwz.annotation.Message;
import lombok.Data;

/**
 * @author liweizhou 2020/4/12
 */
@Data
@Message
public class HelloRequest {

    @Field(1)
    private String name;

    @Field(2)
    private int age;

}

package com.lwz.server.message;

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
    private String host;

    @Field(2)
    private int port;

}

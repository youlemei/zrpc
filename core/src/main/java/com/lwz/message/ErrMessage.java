package com.lwz.message;

import com.lwz.annotation.Field;
import com.lwz.annotation.Message;
import lombok.Data;

/**
 * @author liweizhou 2020/4/11
 */
@Data
@Message
public class ErrMessage {

    @Field(1)
    private String message;

    @Field(2)
    private String exception;

    public static ErrMessage fromException(Exception e) {
        return new ErrMessage();
    }
}

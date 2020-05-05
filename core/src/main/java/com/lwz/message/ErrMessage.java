package com.lwz.message;

import com.lwz.annotation.Field;
import com.lwz.annotation.Message;

/**
 * @author liweizhou 2020/4/11
 */
@Message
public class ErrMessage {

    @Field(1)
    private String message;

    @Field(2)
    private String exception;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }
}

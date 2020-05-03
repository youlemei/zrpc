package com.lwz.server;

/**
 * @author liweizhou 2020/5/3
 */
public class HandlerException extends RuntimeException {

    public HandlerException() {
    }

    public HandlerException(String message) {
        super(message);
    }

    public HandlerException(String message, Throwable cause) {
        super(message, cause);
    }

    public HandlerException(Throwable cause) {
        super(cause);
    }
}

package com.lwz.client;

/**
 * @author liweizhou 2020/5/1
 */
public class FallbackException extends RuntimeException {

    public static final FallbackException FALLBACK_NOT_FOUND = new FallbackException();

    public FallbackException() {
    }

    public FallbackException(String message) {
        super(message);
    }

    public FallbackException(String message, Throwable cause) {
        super(message, cause);
    }

    public FallbackException(Throwable cause) {
        super(cause);
    }
}

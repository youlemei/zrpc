package com.lwz.client.pool;

/**
 * @author liweizhou 2020/5/1
 */
public class FallbackContext {

    private Throwable cause;

    public Throwable getCause() {
        return cause;
    }

    public void setCause(Throwable cause) {
        this.cause = cause;
    }
}

package com.lwz.client;

/**
 * 标记接口
 *
 * @author liweizhou 2020/5/1
 */
public interface ClientFallback {

    ThreadLocal<FallbackContext> FALLBACK_THREAD_LOCAL = new ThreadLocal();

    default Throwable getException() {
        return FALLBACK_THREAD_LOCAL.get().getCause();
    }

}

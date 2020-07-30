package com.lwz.server;

import java.util.HashMap;
import java.util.Map;

/**
 * @author liweizhou 2020/5/9
 */
public class HandlerContext {

    private static final ThreadLocal<Map<String, Object>> THREAD_LOCAL = new ThreadLocal<>();

    static void set(String key, Object value) {
        if (THREAD_LOCAL.get() == null) {
            THREAD_LOCAL.set(new HashMap<>());
        }
        THREAD_LOCAL.get().put(key, value);
    }

    public static Object get(String key) {
        if (THREAD_LOCAL.get() == null) {
            return null;
        }
        return THREAD_LOCAL.get().get(key);
    }

    static void remove() {
        THREAD_LOCAL.remove();
    }

}

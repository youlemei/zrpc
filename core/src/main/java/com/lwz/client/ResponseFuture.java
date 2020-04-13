package com.lwz.client;

import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * @author liweizhou 2020/4/12
 */
public interface ResponseFuture<T> extends Future<T> {

    ResponseFuture<T> onSuccess(Consumer<T> success);
    
    ResponseFuture<T> onFail(Consumer<T> fail);
    
}

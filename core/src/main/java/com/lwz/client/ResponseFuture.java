package com.lwz.client;

import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.SuccessCallback;

import java.util.concurrent.Future;

/**
 * @author liweizhou 2020/4/12
 */
public interface ResponseFuture<T> extends Future<T> {

    ResponseFuture<T> onSuccess(SuccessCallback<T> success);
    
    ResponseFuture<T> onFail(FailureCallback fail);
    
}

package com.lwz.client;

import com.lwz.codec.Messager;
import io.netty.buffer.ByteBuf;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.ListenableFutureCallbackRegistry;
import org.springframework.util.concurrent.SuccessCallback;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author liweizhou 2020/3/29
 */
public class ResponseFutureImpl<T> implements ResponseFuture<T> {

    private volatile T data;

    private volatile Status status = Status.RUN;

    private volatile Throwable e;

    private List<Thread> interruptThreads;

    private Class<T> returnType;

    private LocalDateTime create = LocalDateTime.now();

    private AtomicReference<Result<T>> result = new AtomicReference<>();

    private static final Result SUCCESS = new Result<>();

    static class Result<T> {
        T data;
    }

    enum Status {
        RUN, DONE, CANCEL
    }

    public ResponseFutureImpl() {
    }

    public ResponseFutureImpl(Class<T> returnType) {
        this.returnType = returnType;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (status == Status.DONE) {
            return false;
        }
        if (status == Status.RUN) {
            synchronized (this) {
                if (status == Status.DONE) {
                    return false;
                }
                if (status == Status.RUN) {
                    status = Status.CANCEL;
                    if (mayInterruptIfRunning && interruptThreads != null) {
                        for (Thread thread : interruptThreads) {
                            thread.interrupt();
                        }
                    }
                }
            }
        }
        return true;
    }

    @Override
    public boolean isCancelled() {
        return status == Status.CANCEL;
    }

    @Override
    public boolean isDone() {
        return status == Status.DONE;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        if (status == Status.DONE) {
            return data;
        }
        checkCancel();
        synchronized (this) {
            if (status == Status.DONE) {
                return data;
            }
            checkCancel();
            addInterruptThread();
            this.wait();
            removeInterruptThread();
            checkCancel();
            if (e != null) {
                throw new ExecutionException(e);
            }
            return data;
        }
    }

    private void checkCancel() {
        if (status == Status.CANCEL) {
            throw new CancellationException("ResponseFuture is cancelled.");
        }
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (status == Status.DONE) {
            return data;
        }
        checkCancel();
        synchronized (this) {
            if (status == Status.DONE) {
                return data;
            }
            checkCancel();
            addInterruptThread();
            this.wait(unit.toMillis(timeout));
            removeInterruptThread();
            checkCancel();
            if (e != null) {
                throw new ExecutionException(e);
            }
            if (status == Status.RUN) {
                throw new TimeoutException(String.format("ResponseFuture timeout %d millis.", unit.toMillis(timeout)));
            }
            return data;
        }
    }

    private void addInterruptThread() {
        if (interruptThreads == null) {
            interruptThreads = new LinkedList<>();
        }
        interruptThreads.add(Thread.currentThread());
    }

    private void removeInterruptThread() {
        interruptThreads.remove(Thread.currentThread());
    }

    public void fail(Throwable cause) {
        //报告失败
        synchronized (this) {
            this.e = cause;
            if (this.status == Status.RUN) {
                this.status = Status.DONE;
            }
            this.notifyAll();
            futureCallbackRegistry.failure(cause);
            //onSuccess
            //onFail
        }
    }

    public void success(T data) {
        synchronized (this) {
            if (returnType != null) {
                ByteBuf byteBuf = (ByteBuf) data;
                this.data = Messager.read(byteBuf, returnType);
            }
            if (this.status == Status.RUN) {
                this.status = Status.DONE;
            }
            this.notifyAll();
            futureCallbackRegistry.success(this.data);
            //onSuccess
            //onFail
        }
    }

    public LocalDateTime getCreate() {
        return create;
    }

    private ListenableFutureCallbackRegistry futureCallbackRegistry = new ListenableFutureCallbackRegistry();

    @Override
    public ResponseFuture<T> onSuccess(SuccessCallback<T> success) {
        futureCallbackRegistry.addSuccessCallback(success);
        return this;
    }

    @Override
    public ResponseFuture<T> onFail(FailureCallback fail) {
        futureCallbackRegistry.addFailureCallback(fail);
        return this;
    }
}

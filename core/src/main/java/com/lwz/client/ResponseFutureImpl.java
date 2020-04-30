package com.lwz.client;

import com.lwz.codec.Messager;
import io.netty.buffer.ByteBuf;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.ListenableFutureCallbackRegistry;
import org.springframework.util.concurrent.SuccessCallback;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author liweizhou 2020/3/29
 */
public class ResponseFutureImpl<T> implements ResponseFuture<T> {

    private volatile T data;

    private volatile Status status = Status.RUN;

    private volatile ExecutionException e;

    private Object lock = new Object();

    private List<Thread> interruptThreads;

    private Class<T> returnType;

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
            synchronized (lock) {
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
        synchronized (lock) {
            if (status == Status.DONE) {
                return data;
            }
            checkCancel();
            addInterruptThread();
            lock.wait();
            removeInterruptThread();
            checkCancel();
            if (e != null) {
                throw e;
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
        synchronized (lock) {
            if (status == Status.DONE) {
                return data;
            }
            checkCancel();
            addInterruptThread();
            lock.wait(unit.toMillis(timeout));
            removeInterruptThread();
            checkCancel();
            if (e != null) {
                throw e;
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
        synchronized (lock) {
            this.e = new ExecutionException(cause);
            if (this.status == Status.RUN) {
                this.status = Status.DONE;
            }
            lock.notifyAll();
            futureCallbackRegistry.failure(cause);
            //onSuccess
            //onFail
        }
    }

    //TODO: 超时问题, 容易无限等待
    public void success(T data) {
        synchronized (lock) {
            readResp(data);
            if (this.status == Status.RUN) {
                this.status = Status.DONE;
            }
            lock.notifyAll();
            futureCallbackRegistry.success(this.data);
            //onSuccess
            //onFail
        }
    }

    private void readResp(T data) {
        if (returnType == null) {
            return;
        }
        if (void.class.equals(returnType)) {
            return;
        }
        ByteBuf byteBuf = (ByteBuf) data;
        this.data = Messager.read(byteBuf, returnType);
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

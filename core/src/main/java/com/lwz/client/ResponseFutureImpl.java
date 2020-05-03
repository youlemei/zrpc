package com.lwz.client;

import org.springframework.util.CollectionUtils;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.ListenableFutureCallbackRegistry;
import org.springframework.util.concurrent.SuccessCallback;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author liweizhou 2020/3/29
 */
public class ResponseFutureImpl implements ResponseFuture {

    private List<Thread> interruptThreads;

    private Type returnType;

    private LocalDateTime create = LocalDateTime.now();

    private ListenableFutureCallbackRegistry futureCallbackRegistry = new ListenableFutureCallbackRegistry();

    private AtomicReference<Object> resultReference = new AtomicReference<>();

    private static final Object SUCCESS = new Object();

    private static final Object CANCEL = new Object();

    public ResponseFutureImpl() {
    }

    public ResponseFutureImpl(Type returnType) {
        this.returnType = returnType;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (isDone0()) {
            return false;
        }
        if (resultReference.compareAndSet(null, CANCEL)) {
            if (mayInterruptIfRunning && interruptThreads != null) {
                for (Thread thread : interruptThreads) {
                    thread.interrupt();
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled0();
    }

    @Override
    public boolean isDone() {
        return isDone0();
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        if (!isDone0()) {
            await();
        }
        Object ret = resultReference.get();
        if (ret == CANCEL) {
            throw new CancellationException("ResponseFuture is cancelled.");
        }
        if (ret instanceof Throwable) {
            throw new ExecutionException((Throwable) ret);
        }
        return ret == SUCCESS ? null : ret;
    }

    private void await() throws InterruptedException {
        synchronized (this) {
            while (!isDone0()) {
                addInterruptThread();
                this.wait();
                removeInterruptThread();
            }
        }
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (!isDone0()) {
            if (!await(unit.toMillis(timeout))){
                throw new TimeoutException(String.format("ResponseFuture timeout create:%s now:%s", create, LocalDateTime.now()));
            }
        }
        Object ret = resultReference.get();
        if (ret == CANCEL) {
            throw new CancellationException("ResponseFuture is cancelled.");
        }
        if (ret instanceof Throwable) {
            throw new ExecutionException((Throwable) ret);
        }
        return ret == SUCCESS ? null : ret;
    }

    private boolean await(long millis) throws InterruptedException {
        synchronized (this) {
            if (isDone0()) {
                return true;
            }
            addInterruptThread();
            this.wait(millis);
            removeInterruptThread();
            return isDone0();
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

    private boolean isDone0(){
        return resultReference.get() != null;
    }

    private boolean isCancelled0(){
        return resultReference.get() == CANCEL;
    }

    @Override
    public boolean isSuccess() {
        Object ret = resultReference.get();
        return ret != null && ret != CANCEL && !(ret instanceof Throwable);
    }

    public void success(Object data) {
        if (resultReference.compareAndSet(null, data == null ? SUCCESS : data)) {
            synchronized (this) {
                if (!CollectionUtils.isEmpty(interruptThreads)) {
                    this.notifyAll();
                }
            }
            //async
            futureCallbackRegistry.success(data);
        } else {
            throw new IllegalStateException("ResponseFuture complete already");
        }
    }

    public void fail(Throwable cause) {
        if (resultReference.compareAndSet(null, Objects.requireNonNull(cause, "cause must be null"))) {
            synchronized (this) {
                if (!CollectionUtils.isEmpty(interruptThreads)) {
                    this.notifyAll();
                }
            }
            futureCallbackRegistry.failure(cause);
        } else {
            throw new IllegalStateException("ResponseFuture complete already");
        }
    }

    @Override
    public ResponseFuture onSuccess(SuccessCallback success) {
        futureCallbackRegistry.addSuccessCallback(success);
        return this;
    }

    @Override
    public ResponseFuture onFail(FailureCallback fail) {
        futureCallbackRegistry.addFailureCallback(fail);
        return this;
    }

    public LocalDateTime getCreate() {
        return create;
    }

    public Type getReturnType() {
        return returnType;
    }
}

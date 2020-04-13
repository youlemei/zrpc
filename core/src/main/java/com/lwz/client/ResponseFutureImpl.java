package com.lwz.client;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * @author liweizhou 2020/3/29
 */
public class ResponseFutureImpl<T> implements ResponseFuture<T> {

    private volatile T data;

    private volatile Status status = Status.RUN;

    private volatile ExecutionException e;

    private Object lock = new Object();

    private List<Thread> interruptThreads;

    enum Status {
        RUN, DONE, CANCEL
    }

    public ResponseFutureImpl() {
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (status == Status.DONE) {
            return false;
        }
        if (status == Status.RUN) {
            synchronized (lock) {
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
        synchronized (lock) {
            if (status == Status.DONE) {
                return data;
            }
            if (status == Status.CANCEL) {
                throw new CancellationException("ResponseFuture is cancelled.");
            }
            addInterruptThread();
            lock.wait();
            removeInterruptThread();
            if (e != null) {
                throw e;
            }
            return data;
        }
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (status == Status.DONE) {
            return data;
        }
        synchronized (lock) {
            if (status == Status.DONE) {
                return data;
            }
            if (status == Status.CANCEL) {
                throw new CancellationException("ResponseFuture is cancelled.");
            }
            addInterruptThread();
            lock.wait(unit.toMillis(timeout));
            removeInterruptThread();
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

    public void complete(T data) {
        synchronized (lock) {
            if (this.status == Status.RUN) {
                this.status = Status.DONE;
            }
            this.data = data;
            lock.notifyAll();
            //onSuccess
            //onFail
        }
    }

    //TODO: how to do ?
    @Override
    public ResponseFuture<T> onSuccess(Consumer<T> success) {
        return this;
    }

    @Override
    public ResponseFuture<T> onFail(Consumer<T> fail) {
        return this;
    }
}

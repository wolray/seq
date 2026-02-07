package com.github.wolray.seq;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author wolray
 */
public abstract class AsyncSeq<T> implements Seq<T> {
    protected Object task;
    protected boolean cancelled;
    protected final Async async;
    protected final Seq<T> source;

    AsyncSeq(Async async, Seq<T> source) {
        this.async = async;
        this.source = source;
    }

    public void cancel() {
        cancelled = true;
        joinTask();
    }

    public void joinTask() {
        if (task != null) {
            async.join(task);
        }
    }

    protected void checkState() {
        if (task != null) {
            throw new IllegalStateException("AsyncSeq can only consume once");
        }
    }

    public AsyncSeq<T> onStart(Runnable runnable) {
        return new AsyncSeq<T>(async, source) {
            @Override
            public boolean until(Predicate<T> stop) {
                runnable.run();
                return source.until(stop);
            }
        };
    }

    public AsyncSeq<T> onCompletion(Runnable runnable) {
        return new AsyncSeq<T>(async, source) {
            @Override
            public boolean until(Predicate<T> stop) {
                boolean flag = source.until(stop);
                runnable.run();
                return flag;
            }
        };
    }

    @Override
    public <E> AsyncSeq<E> map(Function<T, E> function) {
        return new AsyncSeq<E>(async, source.map(function)) {
            @Override
            public boolean until(Predicate<E> stop) {
                return source.until(stop);
            }
        };
    }
}

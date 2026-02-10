package com.github.wolray.seq;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author wolray
 */
public abstract class AsyncSeq<T> implements Seq<T> {
    protected final Async async;
    protected final Seq<T> source;

    protected Object task;
    protected boolean cancelled;

    AsyncSeq(Async async, Seq<T> source) {
        this.async = async;
        this.source = source;
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

    public void cancel() {
        cancelled = true;
        joinTask();
    }

    public void joinTask() {
        if (task != null) {
            async.join(task);
        }
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

    public AsyncSeq<T> onStart(Runnable runnable) {
        return new AsyncSeq<T>(async, source) {
            @Override
            public boolean until(Predicate<T> stop) {
                runnable.run();
                return source.until(stop);
            }
        };
    }

    protected void checkState() {
        if (task != null) {
            throw new IllegalStateException("AsyncSeq can only consume once");
        }
    }
}

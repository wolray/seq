package com.github.wolray.seq;

import java.util.function.Consumer;
import java.util.function.Function;

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
        joinConsume();
    }

    public void joinConsume() {
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
            public void consume(Consumer<T> consumer) {
                runnable.run();
                AsyncSeq.this.consume(consumer);
            }
        };
    }

    public AsyncSeq<T> onCompletion(Runnable runnable) {
        return new AsyncSeq<T>(async, source) {
            @Override
            public void consume(Consumer<T> consumer) {
                AsyncSeq.this.consume(consumer);
                runnable.run();
            }
        };
    }

    @Override
    public <E> AsyncSeq<E> map(Function<T, E> function) {
        return new AsyncSeq<E>(async, source.map(function)) {
            @Override
            public void consume(Consumer<E> consumer) {
                AsyncSeq.this.consume(t -> consumer.accept(function.apply(t)));
            }
        };
    }
}

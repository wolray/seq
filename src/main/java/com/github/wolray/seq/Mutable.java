package com.github.wolray.seq;

import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

/**
 * @author wolray
 */
public class Mutable<T> implements Lazy<T> {
    protected boolean isSet = false;
    protected T it;

    public Mutable(T it) {
        this.it = it;
    }

    protected void eval() {}

    protected void eval(ForkJoinPool pool) {
        eval();
    }

    @Override
    public final T get() {
        if (isSet) {
            return it;
        }
        eval();
        isSet = true;
        return it;
    }

    @Override
    public synchronized final T forkJoin(ForkJoinPool pool) {
        if (isSet) {
            return it;
        }
        eval(pool);
        isSet = true;
        return it;
    }

    @Override
    public T set(T value) {
        isSet = true;
        return it = value;
    }

    @Override
    public boolean isSet() {
        return isSet;
    }

    public Optional<T> toOptional() {
        return isSet ? Optional.ofNullable(it) : Optional.empty();
    }
}

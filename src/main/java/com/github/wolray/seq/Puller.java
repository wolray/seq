package com.github.wolray.seq;

import java.util.Iterator;
import java.util.function.Consumer;

/**
 * @author wolray
 */
public abstract class Puller<T> implements Iterator<T> {
    protected T next;
    protected int index = 0;

    @Override
    public final T next() {
        return next;
    }

    @Override
    public void forEachRemaining(Consumer<? super T> action) {
        while (hasNext()) {
            action.accept(next);
        }
    }

    public final boolean pop(Iterator<T> iterator) {
        if (iterator.hasNext()) {
            next = iterator.next();
            return true;
        }
        return false;
    }

    public final boolean set(T value) {
        next = value;
        return true;
    }

    public final boolean setAndIncrease(T value) {
        next = value;
        index++;
        return true;
    }
}

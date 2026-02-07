package com.github.wolray.seq;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author wolray
 */
public abstract class Puller<T> implements Iterator<T> {
    int index = 0;
    T next;

    static <T> Puller<T> flatIterable(Iterable<? extends Iterable<T>> iterable) {
        return new Puller<T>() {
            final Iterator<? extends Iterable<T>> iterator = iterable.iterator();
            Iterator<T> cur = Collections.emptyIterator();

            @Override
            public boolean hasNext() {
                while (!cur.hasNext()) {
                    if (iterator.hasNext()) {
                        cur = iterator.next().iterator();
                    } else {
                        return false;
                    }
                }
                return pop(cur);
            }
        };
    }

    public static <T, E> Puller<E> map(Iterator<T> iterator, Function<T, E> function) {
        return new Puller<E>() {
            @Override
            public boolean hasNext() {
                if (iterator.hasNext()) {
                    return set(function.apply(iterator.next()));
                }
                return false;
            }
        };
    }

    public static <T> Puller<T> zip(Iterator<T> iterator, T t) {
        return new Puller<T>() {
            boolean flag = false;

            @Override
            public boolean hasNext() {
                flag = !flag;
                return flag ? pop(iterator) : set(t);
            }
        };
    }

    @Override
    public void forEachRemaining(Consumer<? super T> action) {
        while (hasNext()) {
            action.accept(next);
        }
    }

    public final int index() {
        return index;
    }

    @Override
    public final T next() {
        return next;
    }

    public final boolean pop(Iterator<T> iterator) {
        if (iterator.hasNext()) {
            next = iterator.next();
            index++;
            return true;
        }
        return false;
    }

    public final boolean set(T value) {
        next = value;
        index++;
        return true;
    }
}

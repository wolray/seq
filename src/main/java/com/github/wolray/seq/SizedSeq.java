package com.github.wolray.seq;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author wolray
 */
public interface SizedSeq<T> extends ItrSeq<T> {
    boolean isEmpty();
    int size();

    @Override
    default SizedSeq<T> cache() {
        return this;
    }

    @Override
    default void consume(Consumer<T> consumer, int n, Consumer<T> substitute) {
        if (n >= size()) {
            consume(substitute);
        } else {
            ItrSeq.super.consume(consumer, n, substitute);
        }
    }

    @Override
    default int count() {
        return size();
    }

    @Override
    default ItrSeq<T> drop(int n) {
        return n >= size() ? Collections::emptyIterator : ItrSeq.super.drop(n);
    }

    default boolean isNotEmpty() {
        return !isEmpty();
    }

    @Override
    default <E> SizedSeq<E> map(Function<T, E> function) {
        return new SizedSeq<E>() {
            @Override
            public Iterator<E> iterator() {
                return ItrUtil.map(SizedSeq.this.iterator(), function);
            }

            @Override
            public int size() {
                return SizedSeq.this.size();
            }

            @Override
            public boolean isEmpty() {
                return SizedSeq.this.isEmpty();
            }

            @Override
            public SizedSeq<E> cache() {
                return toList();
            }
        };
    }

    @Override
    default <E> ItrSeq<E> map(Function<T, E> function, int n, Function<T, E> substitute) {
        if (n >= size()) {
            return map(substitute);
        } else {
            return ItrSeq.super.map(function, n, substitute);
        }
    }

    @Override
    default int sizeOrDefault() {
        return size();
    }

    @Override
    default ItrSeq<T> take(int n) {
        return n >= size() ? this : ItrSeq.super.take(n);
    }
}

package com.github.wolray.seq;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.Function;

public interface SizedSeq<T> extends ItrSeq<T> {
    int size();

    @Override
    default ItrSeq<T> drop(int n) {
        int size = size();
        if (n >= size) {
            return empty();
        } else if (n <= 0) {
            return this;
        } else {
            return of(size - n, ItrSeq.super.drop(n));
        }
    }

    @Override
    default SizedSeq<T> cache() {
        return this;
    }

    @Override
    default <E> SizedSeq<E> map(Function<T, E> function) {
        return of(size(), ItrSeq.super.map(function));
    }

    @Override
    default SizedSeq<T> take(int n) {
        int size = size();
        if (n >= size) {
            return this;
        } else if (n <= 0) {
            return empty();
        } else {
            return of(n, ItrSeq.super.take(n));
        }
    }

    @Override
    default int count() {
        return size();
    }

    @Override
    default int sizeOrDefault() {
        return size();
    }

    static <T> SizedSeq<T> empty() {
        return of(0, Collections::emptyIterator);
    }

    static <T> SizedSeq<T> of(int size, Iterable<T> iterable) {
        return new SizedSeq<T>() {
            @Override
            public Iterator<T> iterator() {
                return iterable.iterator();
            }

            @Override
            public int size() {
                return size;
            }
        };
    }
}

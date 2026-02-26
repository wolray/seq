package com.github.wolray.seq;

@FunctionalInterface
public interface IntObjConsumer<T> {
    void accept(int i, T t);
}

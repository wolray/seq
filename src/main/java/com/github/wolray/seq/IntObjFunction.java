package com.github.wolray.seq;

@FunctionalInterface
public interface IntObjFunction<T, E> {
    E apply(int i, T t);
}

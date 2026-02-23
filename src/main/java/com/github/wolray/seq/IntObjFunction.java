package com.github.wolray.seq;

public interface IntObjFunction<T, E> {
    E apply(int i, T t);
}

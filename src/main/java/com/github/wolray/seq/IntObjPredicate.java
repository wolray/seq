package com.github.wolray.seq;

@FunctionalInterface
public interface IntObjPredicate<T> {
    boolean test(int i, T t);
}

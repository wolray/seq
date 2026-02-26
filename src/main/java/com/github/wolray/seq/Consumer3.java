package com.github.wolray.seq;

/**
 * @author wolray
 */
@FunctionalInterface
public interface Consumer3<T, K, V> {
    void accept(T t, K k, V v);
}

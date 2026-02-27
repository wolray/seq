package com.github.wolray.seq;

import java.util.function.IntPredicate;

public interface IntReducer<V> extends IntPredicate {
    V result();
}

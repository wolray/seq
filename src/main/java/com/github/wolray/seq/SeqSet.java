package com.github.wolray.seq;

import java.util.Set;
import java.util.function.Consumer;

/**
 * @author wolray
 */
public interface SeqSet<T> extends Set<T>, Seq<T> {
    @Override
    default void consume(Consumer<T> consumer) {
        forEach(consumer);
    }

    static <T> SeqSet<T> of(Set<T> set) {
        return set instanceof SeqSet ? (SeqSet<T>)set : new BackedSeqSet<>(set);
    }

    default boolean isNotEmpty() {
        return !isEmpty();
    }
}

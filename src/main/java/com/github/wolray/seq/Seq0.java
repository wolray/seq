package com.github.wolray.seq;

/**
 * @author wolray
 */
public interface Seq0<C> {
    void consume(C consumer);

    default void consumeTillStop(C consumer) {
        try {
            consume(consumer);
        } catch (StopException ignore) {}
    }
}

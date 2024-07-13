package com.github.wolray.seq;

import java.util.function.*;
import java.util.stream.Collector;

/**
 * @author wolray
 */
public interface Transducer<T, V, E> {
    Reducer<T, V> reducer();
    Function<V, E> transformer();

    static <T> Transducer<T, ?, T> of(BinaryOperator<T> binaryOperator) {
        return of(() -> new Mutable<T>(null), (m, t) -> {
            if (m.isSet) {
                m.it = binaryOperator.apply(m.it, t);
            } else {
                m.set(t);
            }
        }, Mutable::get);
    }

    static <T, V, E> Transducer<T, V, E> of(Collector<T, V, E> collector) {
        return of(Reducer.of(collector.supplier(), collector.accumulator()), collector.finisher());
    }

    static <T, V, E> Transducer<T, V, E> of(Reducer<T, V> reducer, Function<V, E> transformer) {
        return new Transducer<T, V, E>() {
            @Override
            public Reducer<T, V> reducer() {
                return reducer;
            }

            @Override
            public Function<V, E> transformer() {
                return transformer;
            }
        };
    }

    static <T, V, E> Transducer<T, V, E> of(Supplier<V> supplier, BiConsumer<V, T> accumulator, Function<V, E> transformer) {
        return of(Reducer.of(supplier, accumulator), transformer);
    }
}

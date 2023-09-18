package com.github.wolray.seq;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * @author wolray
 */
public interface Transducer<T, V, E> {
    Reducer<T, V> reducer();
    Function<V, E> transformer();

    static <T, V, E> Transducer<T, V, E> filtering(Predicate<T> predicate, Transducer<T, V, E> transducer) {
        return of(Reducer.filtering(predicate, transducer.reducer()), transducer.transformer());
    }

    static <T, R, V, E> Transducer<T, V, E> mapping(Function<T, R> mapper, Transducer<R, V, E> transducer) {
        return of(Reducer.mapping(mapper, transducer.reducer()), transducer.transformer());
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

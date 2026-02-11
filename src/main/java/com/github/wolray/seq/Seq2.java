package com.github.wolray.seq;

import java.util.Comparator;
import java.util.Map;
import java.util.function.*;

/**
 * @author wolray
 */
public interface Seq2<K, V> {
    boolean until(BiPredicate<K, V> predicate);

    static <K, V> Seq2<K, V> empty() {
        return p -> false;
    }

    static <K, V> Seq2<K, V> of(Map<K, V> map) {
        return p -> {
            for (Map.Entry<K, V> e : map.entrySet()) {
                if (p.test(e.getKey(), e.getValue())) {
                    return true;
                }
            }
            return false;
        };
    }

    static <K, V> Seq2<K, V> unit(K k, V v) {
        return p -> p.test(k, v);
    }

    default void consume(BiConsumer<K, V> consumer) {
        until((k, v) -> {
            consumer.accept(k, v);
            return false;
        });
    }

    default <E> E reduce(E des, Consumer3<E, K, V> accumulator) {
        until((k, v) -> {
            accumulator.accept(des, k, v);
            return false;
        });
        return des;
    }

    default <M extends Map<K, V>> M collectBy(M des) {
        consume(des::put);
        return des;
    }

    default Pair<K, V> maxByKey(Comparator<K> comparator) {
        return reduce(new Pair<>(null, null), (p, k, v) -> {
            if (p.first == null || comparator.compare(p.first, k) < 0) {
                p.set(k, v);
            }
        });
    }

    default Pair<K, V> maxByValue(Comparator<V> comparator) {
        return reduce(new Pair<>(null, null), (p, k, v) -> {
            if (p.second == null || comparator.compare(p.second, v) < 0) {
                p.set(k, v);
            }
        });
    }

    default Pair<K, V> minByKey(Comparator<K> comparator) {
        return reduce(new Pair<>(null, null), (p, k, v) -> {
            if (p.first == null || comparator.compare(p.first, k) > 0) {
                p.set(k, v);
            }
        });
    }

    default Pair<K, V> minByValue(Comparator<V> comparator) {
        return reduce(new Pair<>(null, null), (p, k, v) -> {
            if (p.second == null || comparator.compare(p.second, v) > 0) {
                p.set(k, v);
            }
        });
    }

    default <T> Seq<T> map(BiFunction<K, V, T> function) {
        return p -> until((k, v) -> p.test(function.apply(k, v)));
    }

    default Seq<Pair<K, V>> paired() {
        return map(Pair::new);
    }

    default Seq<K> toKeys() {
        return p -> until((k, v) -> p.test(k));
    }

    default Seq<V> toValues() {
        return p -> until((k, v) -> p.test(v));
    }

    default Seq2<K, V> cache() {
        Seq<Pair<K, V>> pairSeq = paired().cache();
        return p -> pairSeq.until(pair -> p.test(pair.first, pair.second));
    }

    default Seq2<K, V> filter(BiPredicate<K, V> predicate) {
        return p -> until((k, v) -> predicate.test(k, v) && p.test(k, v));
    }

    default Seq2<K, V> filterByKey(Predicate<K> predicate) {
        return p -> until((k, v) -> predicate.test(k) && p.test(k, v));
    }

    default Seq2<K, V> filterByValue(Predicate<V> predicate) {
        return p -> until((k, v) -> predicate.test(v) && p.test(k, v));
    }

    default <T> Seq2<T, V> mapKeys(BiFunction<K, V, T> function) {
        return p -> until((k, v) -> p.test(function.apply(k, v), v));
    }

    default <T> Seq2<T, V> mapKeys(Function<K, T> function) {
        return p -> until((k, v) -> p.test(function.apply(k), v));
    }

    default <T> Seq2<K, T> mapValues(BiFunction<K, V, T> function) {
        return p -> until((k, v) -> p.test(k, function.apply(k, v)));
    }

    default <T> Seq2<K, T> mapValues(Function<V, T> function) {
        return p -> until((k, v) -> p.test(k, function.apply(v)));
    }

    default Seq2<K, V> onEach(BiConsumer<K, V> consumer) {
        return p -> until((k, v) -> {
            consumer.accept(k, v);
            return p.test(k, v);
        });
    }

    default Seq2<V, K> swap() {
        return p -> until((k, v) -> p.test(v, k));
    }

    default SeqMap<K, SeqList<V>> groupBy() {
        return groupBy(Reducer.toList());
    }

    default <T> SeqMap<K, T> groupBy(Reducer<V, T> reducer) {
        SeqMap<K, Reducer.Worker<V, T>> map = new SeqMap<>();
        consume((k, v) -> map.getOrCompute(k, reducer::get).accept(v));
        return map.mapValues(Reducer.Worker::result);
    }

    default SeqMap<K, V> toMap() {
        return collectBy(new SeqMap<>());
    }

    default <A, B> SeqMap<A, B> toMap(Consumer3<SeqMap<A, B>, K, V> consumer) {
        SeqMap<A, B> res = new SeqMap<>();
        consume((k, v) -> consumer.accept(res, k, v));
        return res;
    }
}

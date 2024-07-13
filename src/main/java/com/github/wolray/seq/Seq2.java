package com.github.wolray.seq;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.*;

import static com.github.wolray.seq.Splitter.substring;

/**
 * @author wolray
 */
public interface Seq2<K, V> extends Seq0<BiConsumer<K, V>> {
    @SuppressWarnings("unchecked")
    static <K, V> Seq2<K, V> empty() {
        return (Seq2<K, V>)Empty.emptySeq;
    }

    @SuppressWarnings("unchecked")
    static <K, V> BiConsumer<K, V> nothing() {
        return (BiConsumer<K, V>)Empty.nothing;
    }

    static <K, V> Seq2<K, V> of(Map<K, V> map) {
        return map instanceof SeqMap ? (SeqMap<K, V>)map : map::forEach;
    }

    static Seq2<String, String> parseMap(char[] chars, char entrySep, char kvSep) {
        return c -> {
            int len = chars.length, last = 0;
            String prev = null;
            for (int i = 0; i < len; i++) {
                if (chars[i] == entrySep) {
                    if (prev != null) {
                        c.accept(prev, substring(chars, last, i));
                        prev = null;
                    }
                    last = i + 1;
                } else if (prev == null && chars[i] == kvSep) {
                    prev = substring(chars, last, i);
                    last = i + 1;
                }
            }
            if (prev != null) {
                c.accept(prev, substring(chars, last, len));
            }
        };
    }

    static Seq2<String, String> parseMap(String s, char entrySep, char kvSep) {
        return parseMap(s.toCharArray(), entrySep, kvSep);
    }

    static <K, V> Seq2<K, V> unit(K k, V v) {
        return c -> c.accept(k, v);
    }

    default Seq2<K, V> cache() {
        Seq<Pair<K, V>> pairSeq = paired().cache();
        return c -> pairSeq.consume(p -> c.accept(p.first, p.second));
    }

    default Seq2<K, V> filter(BiPredicate<K, V> predicate) {
        return c -> consume((k, v) -> {
            if (predicate.test(k, v)) {
                c.accept(k, v);
            }
        });
    }

    default Seq2<K, V> filterByKey(Predicate<K> predicate) {
        return c -> consume((k, v) -> {
            if (predicate.test(k)) {
                c.accept(k, v);
            }
        });
    }

    default Seq2<K, V> filterByValue(Predicate<V> predicate) {
        return c -> consume((k, v) -> {
            if (predicate.test(v)) {
                c.accept(k, v);
            }
        });
    }

    default <E> E fold(E init, Function3<E, K, V, E> function) {
        Mutable<E> m = new Mutable<>(init);
        consume((k, v) -> m.it = function.apply(m.it, k, v));
        return m.it;
    }

    default Seq<K> justKeys() {
        return c -> consume((k, v) -> c.accept(k));
    }

    default Seq<V> justValues() {
        return c -> consume((k, v) -> c.accept(v));
    }

    default <T> Seq<T> map(BiFunction<K, V, T> function) {
        return c -> consume((k, v) -> c.accept(function.apply(k, v)));
    }

    default <T> Seq2<T, V> mapKey(BiFunction<K, V, T> function) {
        return c -> consume((k, v) -> c.accept(function.apply(k, v), v));
    }

    default <T> Seq2<T, V> mapKey(Function<K, T> function) {
        return c -> consume((k, v) -> c.accept(function.apply(k), v));
    }

    default <T> Seq2<K, T> mapValue(BiFunction<K, V, T> function) {
        return c -> consume((k, v) -> c.accept(k, function.apply(k, v)));
    }

    default <T> Seq2<K, T> mapValue(Function<V, T> function) {
        return c -> consume((k, v) -> c.accept(k, function.apply(v)));
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

    default Seq2<K, V> onEach(BiConsumer<K, V> consumer) {
        return c -> consumeTillStop(consumer.andThen(c));
    }

    default Seq<Pair<K, V>> paired() {
        return map(Pair::new);
    }

    default <E> E reduce(E des, Consumer3<E, K, V> accumulator) {
        consume((k, v) -> accumulator.accept(des, k, v));
        return des;
    }

    default Seq2<V, K> swap() {
        return c -> consume((k, v) -> c.accept(v, k));
    }

    default SeqMap<K, V> toMap() {
        return toMap(new LinkedHashMap<>());
    }

    default SeqMap<K, V> toMap(Map<K, V> des) {
        return SeqMap.of(reduce(des, Map::put));
    }

    default <A, B> SeqMap<A, B> toMap(BiFunction<K, V, A> toKey, BiFunction<K, V, B> toValue) {
        return toMap(new LinkedHashMap<>(), toKey, toValue);
    }

    default <A, B> SeqMap<A, B> toMap(Map<A, B> des, BiFunction<K, V, A> toKey, BiFunction<K, V, B> toValue) {
        return SeqMap.of(reduce(des, (res, k, v) -> res.put(toKey.apply(k, v), toValue.apply(k, v))));
    }

    class Empty {
        static final Seq2<Object, Object> emptySeq = c -> {};
        static final BiConsumer<Object, Object> nothing = (k, v) -> {};
    }
}

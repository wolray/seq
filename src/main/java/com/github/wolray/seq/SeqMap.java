package com.github.wolray.seq;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author wolray
 */
public class SeqMap<K, V> extends LinkedHashMap<K, V> implements Seq2<K, V> {
    public SeqMap() {}

    public SeqMap(Map<? extends K, ? extends V> m) {
        super(m);
    }

    public SeqMap(int initialCapacity) {
        super(initialCapacity);
    }

    @Override
    public ItrSeq<K> toKeys() {
        return Seq.of(keySet());
    }

    @Override
    public ItrSeq<V> toValues() {
        return Seq.of(values());
    }

    @Override
    public <T> SeqMap<T, V> mapKeys(BiFunction<K, V, T> toKey) {
        SeqMap<T, V> res = new SeqMap<>(size());
        forEach((k, v) -> res.put(toKey.apply(k, v), v));
        return res;
    }

    @Override
    public <T> SeqMap<T, V> mapKeys(Function<K, T> toKey) {
        SeqMap<T, V> res = new SeqMap<>(size());
        forEach((k, v) -> res.put(toKey.apply(k), v));
        return res;
    }

    @Override
    public <T> SeqMap<K, T> mapValues(BiFunction<K, V, T> toValue) {
        SeqMap<K, T> res = new SeqMap<>(size());
        forEach((k, v) -> res.put(k, toValue.apply(k, v)));
        return res;
    }

    @Override
    public <T> SeqMap<K, T> mapValues(Function<V, T> toValue) {
        SeqMap<K, T> res = new SeqMap<>(size());
        forEach((k, v) -> res.put(k, toValue.apply(v)));
        return res;
    }

    @Override
    public SeqMap<K, V> toMap() {
        return new SeqMap<>(this);
    }

    @Override
    public boolean until(BiPredicate<K, V> predicate) {
        for (Map.Entry<K, V> entry : entrySet()) {
            if (predicate.test(entry.getKey(), entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    public static <K, V> SeqMap<K, V> of(Map<K, V> map) {
        return map instanceof SeqMap ? (SeqMap<K, V>)map : new SeqMap<>(map);
    }

    public ItrSeq<Map.Entry<K, V>> toEntries() {
        return Seq.of(entrySet());
    }

    public V getOrCompute(K key, Supplier<? extends V> supplier) {
        return computeIfAbsent(key, k -> supplier.get());
    }
}

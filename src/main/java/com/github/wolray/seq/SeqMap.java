package com.github.wolray.seq;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author wolray
 */
public class SeqMap<K, V> extends LinkedHashMap<K, V> {
    public SeqMap() {}

    public SeqMap(Map<? extends K, ? extends V> m) {
        super(m);
    }

    public SeqMap(int initialCapacity) {
        super(initialCapacity);
    }

    public static <K, V> SeqMap<K, V> of(Map<K, V> map) {
        return map instanceof SeqMap ? (SeqMap<K, V>)map : new SeqMap<>(map);
    }

    public ItrSeq<Map.Entry<K, V>> seqOfEntries() {
        return Seq.of(entrySet());
    }

    public ItrSeq<K> seqOfKeys() {
        return Seq.of(keySet());
    }

    public ItrSeq<V> seqOfValues() {
        return Seq.of(values());
    }

    public <T> SeqMap<T, V> mapKeys(BiFunction<K, V, T> toKey) {
        SeqMap<T, V> res = new SeqMap<>(size());
        forEach((k, v) -> res.put(toKey.apply(k, v), v));
        return res;
    }

    public <T> SeqMap<T, V> mapKeys(Function<K, T> toKey) {
        SeqMap<T, V> res = new SeqMap<>(size());
        forEach((k, v) -> res.put(toKey.apply(k), v));
        return res;
    }

    public <T> SeqMap<K, T> mapValues(BiFunction<K, V, T> toValue) {
        SeqMap<K, T> res = new SeqMap<>(size());
        forEach((k, v) -> res.put(k, toValue.apply(k, v)));
        return res;
    }

    public <T> SeqMap<K, T> mapValues(Function<V, T> toValue) {
        SeqMap<K, T> res = new SeqMap<>(size());
        forEach((k, v) -> res.put(k, toValue.apply(v)));
        return res;
    }
}

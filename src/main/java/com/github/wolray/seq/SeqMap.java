package com.github.wolray.seq;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author wolray
 */
public class SeqMap<K, V> implements Seq2<K, V>, Map<K, V> {
    public final Map<K, V> backer;
    private SeqSet<K> keySet;
    private SeqCollection<V> values;
    private SeqSet<Entry<K, V>> entrySet;

    SeqMap(Map<K, V> backer) {
        this.backer = backer;
    }

    public static <K, V> SeqMap<K, V> of(Map<K, V> map) {
        return map instanceof SeqMap ? (SeqMap<K, V>)map : new SeqMap<>(map);
    }

    public static <K, V> SeqMap<K, V> hash() {
        return new SeqMap<>(new LinkedHashMap<>());
    }

    public static <K, V> SeqMap<K, V> hash(int initialCapacity) {
        return new SeqMap<>(new LinkedHashMap<>(initialCapacity));
    }

    public static <K, V> SeqMap<K, V> tree(Comparator<K> comparator) {
        return new SeqMap<>(new TreeMap<>(comparator));
    }

    public static <K, V> Map<K, V> newMap(Map<?, ?> map) {
        if (map instanceof LinkedHashMap) {
            return new LinkedHashMap<>(map.size());
        }
        if (map instanceof HashMap) {
            return new HashMap<>(map.size());
        }
        if (map instanceof TreeMap) {
            return new TreeMap<>();
        }
        if (map instanceof ConcurrentHashMap) {
            return new ConcurrentHashMap<>(map.size());
        }
        return new HashMap<>(map.size());
    }

    @Override
    public void consume(BiConsumer<K, V> consumer) {
        backer.forEach(consumer);
    }

    @Override
    public SeqSet<K> keySet() {
        if (keySet == null) {
            keySet = SeqProxy.ofSet(backer.keySet());
        }
        return keySet;
    }

    @Override
    public SeqCollection<V> values() {
        if (values == null) {
            values = SeqProxy.ofCollection(backer.values());
        }
        return values;
    }

    @Override
    public SeqSet<Entry<K, V>> entrySet() {
        if (entrySet == null) {
            entrySet = SeqProxy.ofSet(backer.entrySet());
        }
        return entrySet;
    }

    @Override
    public SeqMap<K, V> toMap() {
        return this;
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }

    public <E extends Comparable<E>> ArraySeq<Entry<K, V>> sort(BiFunction<K, V, E> function) {
        return entrySet().sortBy(e -> function.apply(e.getKey(), e.getValue()));
    }

    public <E extends Comparable<E>> ArraySeq<Entry<K, V>> sortDesc(BiFunction<K, V, E> function) {
        return entrySet().sortByDesc(e -> function.apply(e.getKey(), e.getValue()));
    }

    public ArraySeq<Entry<K, V>> sortByKey(Comparator<K> comparator) {
        return entrySet().sortWith(Entry.comparingByKey(comparator));
    }

    public ArraySeq<Entry<K, V>> sortDescByKey(Comparator<K> comparator) {
        return entrySet().sortWithDesc(Entry.comparingByKey(comparator));
    }

    public ArraySeq<Entry<K, V>> sortByValue(Comparator<V> comparator) {
        return entrySet().sortWith(Entry.comparingByValue(comparator));
    }

    public ArraySeq<Entry<K, V>> sortDescByValue(Comparator<V> comparator) {
        return entrySet().sortWithDesc(Entry.comparingByValue(comparator));
    }

    public <E> SeqMap<E, V> mapByKey(BiFunction<K, V, E> function) {
        return toMap(newMap(backer), function, (k, v) -> v);
    }

    public <E> SeqMap<E, V> mapByKey(Function<K, E> function) {
        return toMap(newMap(backer), (k, v) -> function.apply(k), (k, v) -> v);
    }

    public <E> SeqMap<K, E> mapByValue(BiFunction<K, V, E> function) {
        return toMap(newMap(backer), (k, v) -> k, function);
    }

    public <E> SeqMap<K, E> mapByValue(Function<V, E> function) {
        return toMap(newMap(backer), (k, v) -> k, (k, v) -> function.apply(v));
    }

    @SuppressWarnings("unchecked")
    public <E> SeqMap<K, E> replaceValue(BiFunction<K, V, E> function) {
        SeqMap<K, Object> map = (SeqMap<K, Object>)this;
        map.entrySet().forEach(e -> e.setValue(function.apply(e.getKey(), (V)e.getValue())));
        return (SeqMap<K, E>)map;
    }

    @SuppressWarnings("unchecked")
    public <E> SeqMap<K, E> replaceValue(Function<V, E> function) {
        SeqMap<K, Object> map = (SeqMap<K, Object>)this;
        map.entrySet().forEach(e -> e.setValue(function.apply((V)e.getValue())));
        return (SeqMap<K, E>)map;
    }

    @Override
    public int size() {
        return backer.size();
    }

    @Override
    public boolean isEmpty() {
        return backer.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return backer.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return backer.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return backer.get(key);
    }

    @Override
    public V put(K key, V value) {
        return backer.put(key, value);
    }

    @Override
    public V remove(Object key) {
        return backer.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        backer.putAll(m);
    }

    @Override
    public void clear() {
        backer.clear();
    }

    @Override
    public String toString() {
        return backer.toString();
    }
}

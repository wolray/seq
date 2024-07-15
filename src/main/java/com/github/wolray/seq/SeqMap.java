package com.github.wolray.seq;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author wolray
 */
public interface SeqMap<K, V> extends Seq2<K, V>, Map<K, V> {
    <A, B> SeqMap<A, B> newForMapping();
    SeqSet<Entry<K, V>> seqEntrySet();
    SeqSet<K> seqKeySet();
    SeqCollection<V> seqValues();

    static <K, V> SeqMap<K, V> hash() {
        return new LinkedSeqMap<>();
    }

    static <K, V> SeqMap<K, V> hash(int initialCapacity) {
        return new LinkedSeqMap<>(initialCapacity);
    }

    static <K, V> SeqMap<K, V> of(Map<K, V> map) {
        return map instanceof SeqMap ? (SeqMap<K, V>)map : new Proxy<>(map);
    }

    static <K, V> SeqMap<K, V> tree(Comparator<K> comparator) {
        return new Proxy<>(new TreeMap<>(comparator));
    }

    @Override
    default void consume(BiConsumer<K, V> consumer) {
        forEach(consumer);
    }

    default boolean isNotEmpty() {
        return !isEmpty();
    }

    default <E> SeqMap<E, V> mapByKey(BiFunction<K, V, E> function) {
        return toMap(newForMapping(), function, (k, v) -> v);
    }

    default <E> SeqMap<E, V> mapByKey(Function<K, E> function) {
        return toMap(newForMapping(), (k, v) -> function.apply(k), (k, v) -> v);
    }

    default <E> SeqMap<K, E> mapByValue(BiFunction<K, V, E> function) {
        return toMap(newForMapping(), (k, v) -> k, function);
    }

    default <E> SeqMap<K, E> mapByValue(Function<V, E> function) {
        return toMap(newForMapping(), (k, v) -> k, (k, v) -> function.apply(v));
    }

    @SuppressWarnings("unchecked")
    default <E> SeqMap<K, E> replaceValue(BiFunction<K, V, E> function) {
        SeqMap<K, Object> map = (SeqMap<K, Object>)this;
        map.entrySet().forEach(e -> e.setValue(function.apply(e.getKey(), (V)e.getValue())));
        return (SeqMap<K, E>)map;
    }

    @SuppressWarnings("unchecked")
    default <E> SeqMap<K, E> replaceValue(Function<V, E> function) {
        SeqMap<K, Object> map = (SeqMap<K, Object>)this;
        map.entrySet().forEach(e -> e.setValue(function.apply((V)e.getValue())));
        return (SeqMap<K, E>)map;
    }

    default <E extends Comparable<E>> ArraySeq<Entry<K, V>> sort(BiFunction<K, V, E> function) {
        return seqEntrySet().sortBy(e -> function.apply(e.getKey(), e.getValue()));
    }

    default ArraySeq<Entry<K, V>> sortByKey(Comparator<K> comparator) {
        return seqEntrySet().sortWith(Entry.comparingByKey(comparator));
    }

    default ArraySeq<Entry<K, V>> sortByValue(Comparator<V> comparator) {
        return seqEntrySet().sortWith(Entry.comparingByValue(comparator));
    }

    default <E extends Comparable<E>> ArraySeq<Entry<K, V>> sortDesc(BiFunction<K, V, E> function) {
        return seqEntrySet().sortByDesc(e -> function.apply(e.getKey(), e.getValue()));
    }

    default ArraySeq<Entry<K, V>> sortDescByKey(Comparator<K> comparator) {
        return seqEntrySet().sortWithDesc(Entry.comparingByKey(comparator));
    }

    default ArraySeq<Entry<K, V>> sortDescByValue(Comparator<V> comparator) {
        return seqEntrySet().sortWithDesc(Entry.comparingByValue(comparator));
    }

    @Override
    default SeqMap<K, V> toMap() {
        return this;
    }

    class Proxy<K, V> implements SeqMap<K, V> {
        public final Map<K, V> backer;

        Proxy(Map<K, V> backer) {
            this.backer = backer;
        }

        @Override
        public void consume(BiConsumer<K, V> consumer) {
            backer.forEach(consumer);
        }

        @Override
        public Set<K> keySet() {
            return backer.keySet();
        }

        @Override
        public SeqSet<K> seqKeySet() {
            return SeqSet.of(backer.keySet());
        }

        @Override
        public Collection<V> values() {
            return backer.values();
        }

        @Override
        public SeqCollection<V> seqValues() {
            return SeqCollection.of(backer.values());
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return backer.entrySet();
        }

        @Override
        public SeqSet<Entry<K, V>> seqEntrySet() {
            return SeqSet.of(backer.entrySet());
        }

        @Override
        public <A, B> SeqMap<A, B> newForMapping() {
            if (backer instanceof TreeMap) {
                return new Proxy<>(new TreeMap<>());
            }
            if (backer instanceof ConcurrentHashMap) {
                return new Proxy<>(new ConcurrentHashMap<>(backer.size()));
            }
            return new LinkedSeqMap<>(backer.size());
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
}

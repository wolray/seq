package com.github.wolray.seq;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author wolray
 */
public class LinkedSeqMap<K, V> extends LinkedHashMap<K, V> implements SeqMap<K, V> {
    public LinkedSeqMap(int initialCapacity) {
        super(initialCapacity);
    }

    public LinkedSeqMap() {}

    public LinkedSeqMap(Map<? extends K, ? extends V> m) {
        super(m);
    }

    @Override
    public SeqSet<K> seqKeySet() {
        return SeqSet.of(keySet());
    }

    @Override
    public SeqCollection<V> seqValues() {
        return SeqCollection.of(values());
    }

    @Override
    public SeqSet<Map.Entry<K, V>> seqEntrySet() {
        return SeqSet.of(entrySet());
    }

    @Override
    public <A, B> SeqMap<A, B> newForMapping() {
        return new LinkedSeqMap<>(size());
    }
}

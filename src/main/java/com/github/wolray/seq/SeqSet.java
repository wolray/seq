package com.github.wolray.seq;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * @author wolray
 */
public class SeqSet<T> extends LinkedHashSet<T> implements ItrSeq<T> {
    public SeqSet(int initialCapacity) {
        super(initialCapacity);
    }

    public SeqSet() {}

    public SeqSet(Collection<? extends T> c) {
        super(c);
    }
}

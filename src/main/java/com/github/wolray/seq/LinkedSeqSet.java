package com.github.wolray.seq;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * @author wolray
 */
public class LinkedSeqSet<T> extends LinkedHashSet<T> implements SeqSet<T> {
    public LinkedSeqSet(int initialCapacity) {
        super(initialCapacity);
    }

    public LinkedSeqSet() {}

    public LinkedSeqSet(Collection<? extends T> c) {
        super(c);
    }
}

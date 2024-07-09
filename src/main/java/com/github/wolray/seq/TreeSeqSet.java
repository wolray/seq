package com.github.wolray.seq;

import java.util.Collection;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author wolray
 */
public class TreeSeqSet<T> extends TreeSet<T> implements SeqSet<T> {
    public TreeSeqSet() {}

    public TreeSeqSet(Comparator<? super T> comparator) {
        super(comparator);
    }

    public TreeSeqSet(Collection<? extends T> c) {
        super(c);
    }

    public TreeSeqSet(SortedSet<T> s) {
        super(s);
    }
}

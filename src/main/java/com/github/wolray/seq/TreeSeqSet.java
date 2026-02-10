package com.github.wolray.seq;

import java.util.Collection;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author wolray
 */
public class TreeSeqSet<T> extends TreeSet<T> implements SizedSeq<T> {
    public TreeSeqSet() {}

    public TreeSeqSet(Collection<? extends T> c) {
        super(c);
    }

    public TreeSeqSet(Comparator<? super T> comparator) {
        super(comparator);
    }

    public TreeSeqSet(SortedSet<T> s) {
        super(s);
    }
}

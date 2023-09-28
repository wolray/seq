package com.github.wolray.seq;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author wolray
 */
public class ArraySeq<T> extends ArrayList<T> implements SeqList<T> {
    public ArraySeq(int initialCapacity) {
        super(initialCapacity);
    }

    public ArraySeq() {}

    public ArraySeq(Collection<? extends T> c) {
        super(c);
    }

    public void swap(int i, int j) {
        T t = get(i);
        set(i, get(j));
        set(j, t);
    }
}


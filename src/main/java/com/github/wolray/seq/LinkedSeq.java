package com.github.wolray.seq;

import java.util.Collection;
import java.util.LinkedList;

/**
 * @author wolray
 */
public class LinkedSeq<T> extends LinkedList<T> implements ItrSeq<T> {
    public LinkedSeq() {}

    public LinkedSeq(Collection<? extends T> c) {
        super(c);
    }

    @Override
    public int sizeOrDefault() {
        return size();
    }
}

package com.github.wolray.seq;

import java.util.Collection;
import java.util.LinkedList;

/**
 * @author wolray
 */
public class LinkedSeq<T> extends LinkedList<T> implements SeqList<T>, SeqQueue<T> {
    public LinkedSeq() {}

    public LinkedSeq(Collection<? extends T> c) {
        super(c);
    }
}

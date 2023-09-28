package com.github.wolray.seq;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author wolray
 */
public class ConcurrentSeq<T> extends ConcurrentLinkedQueue<T> implements SeqQueue<T> {
    public ConcurrentSeq() {}

    public ConcurrentSeq(Collection<? extends T> c) {
        super(c);
    }
}

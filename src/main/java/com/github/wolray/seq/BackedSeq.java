package com.github.wolray.seq;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author wolray
 */
public class BackedSeq<T, C extends Collection<T>> implements SizedSeq<T> {
    public final C backer;

    public BackedSeq(C backer) {
        this.backer = backer;
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
    public Iterator<T> iterator() {
        return backer.iterator();
    }
}

package com.github.wolray.seq;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.Predicate;

/**
 * @author wolray
 */
public class BatchedSeq<T> implements SizedSeq<T> {
    private transient int batchSize = 10;
    private transient final LinkedList<ArrayList<T>> list = new LinkedList<>();
    private transient int size;
    private transient ArrayList<T> cur;

    @Override
    public boolean until(Predicate<T> stop) {
        for (ArrayList<T> ts : list) {
            for (T t : ts) {
                if (stop.test(t)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Iterator<T> iterator() {
        return ItrSeq.flatIterable(list);
    }

    @Override
    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void add(T t) {
        if (cur == null) {
            cur = new ArrayList<>(batchSize);
            list.add(cur);
        }
        cur.add(t);
        size++;
        if (cur.size() == batchSize) {
            cur = null;
            batchSize = Math.min(300, Math.max(batchSize, size >> 1));
        }
    }

    @Override
    public String toString() {
        return toList().toString();
    }
}

package com.github.wolray.seq;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;

/**
 * @author wolray
 */
public class SeqList<T> extends ArrayList<T> implements SizedSeq<T> {
    public SeqList() {}

    public SeqList(Collection<? extends T> c) {
        super(c);
    }

    public SeqList(int initialCapacity) {
        super(initialCapacity);
    }

    public void swap(int i, int j) {
        T t = get(i);
        set(i, get(j));
        set(j, t);
    }

    public Seq<SeqList<T>> permute(boolean inplace) {
        return p -> permute(p, inplace, 0);
    }

    private boolean permute(Predicate<SeqList<T>> p, boolean inplace, int i) {
        int n = size();
        if (i == n) {
            return p.test(inplace ? this : new SeqList<>(this));
        }
        for (int j = i; j < n; j++) {
            swap(i, j);
            if (permute(p, inplace, i + 1)) {
                swap(i, j);
                return true;
            }
            swap(i, j);
        }
        return false;
    }
}

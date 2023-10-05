package com.github.wolray.seq;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

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

    public Seq<ArraySeq<T>> permute(boolean inplace) {
        return c -> permute(c, inplace, 0);
    }

    private void permute(Consumer<ArraySeq<T>> c, boolean inplace, int i) {
        int n = size();
        if (i == n) {
            c.accept(inplace ? this : new ArraySeq<>(this));
            return;
        }
        for (int j = i; j < n; j++) {
            swap(i, j);
            permute(c, inplace, i + 1);
            swap(i, j);
        }
    }
}


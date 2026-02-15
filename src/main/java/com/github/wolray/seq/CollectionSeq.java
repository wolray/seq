package com.github.wolray.seq;

import java.util.Collection;

public interface CollectionSeq<T> extends Collection<T>, SizedSeq<T> {
    @Override
    default SeqList<T> toList() {
        return new SeqList<>(this);
    }

    @Override
    default SeqSet<T> toSet() {
        return new SeqSet<>(this);
    }
}

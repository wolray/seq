package com.github.wolray.seq;

public interface SizedSeq<T> extends ItrSeq<T> {
    int size();

    @Override
    default ItrSeq<T> drop(int n) {
        return n >= size() ? Seq.empty() : ItrSeq.super.drop(n);
    }

    @Override
    default ItrSeq<T> take(int n) {
        return n >= size() ? this : ItrSeq.super.take(n);
    }

    @Override
    default SizedSeq<T> cache() {
        return this;
    }

    @Override
    default int count() {
        return size();
    }

    @Override
    default int sizeOrDefault() {
        return size();
    }
}

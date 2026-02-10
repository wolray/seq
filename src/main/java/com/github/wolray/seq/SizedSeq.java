package com.github.wolray.seq;

public interface SizedSeq<T> extends ItrSeq<T> {
    int size();

    @Override
    default int sizeOrDefault() {
        return size();
    }

    @Override
    default int count() {
        return size();
    }

    @Override
    default ItrSeq<T> take(int n) {
        return n >= size() ? this : ItrSeq.super.take(n);
    }

    @Override
    default ItrSeq<T> drop(int n) {
        return n >= size() ? ItrSeq.empty() : ItrSeq.super.drop(n);
    }
}

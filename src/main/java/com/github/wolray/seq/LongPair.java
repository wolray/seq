package com.github.wolray.seq;

/**
 * @author wolray
 */
public class LongPair<T> {
    public long longVal;
    public T it;

    public LongPair(long longVal, T it) {
        this.longVal = longVal;
        this.it = it;
    }

    @Override
    public String toString() {
        return String.format("(%d,%s)", longVal, it);
    }
}

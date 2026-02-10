package com.github.wolray.seq;

/**
 * @author wolray
 */
public class LongPair<T> {
    public T it;
    public long longVal;

    public LongPair(long longVal, T it) {
        this.longVal = longVal;
        this.it = it;
    }

    @Override
    public String toString() {
        return String.format("(%d,%s)", longVal, it);
    }
}

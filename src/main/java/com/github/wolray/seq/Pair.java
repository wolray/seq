package com.github.wolray.seq;

/**
 * @author wolray
 */
public class Pair<A, B> {
    public A first;
    public B second;

    public Pair(A first, B second) {
        set(first, second);
    }

    @Override
    public String toString() {
        return String.format("(%s,%s)", first, second);
    }

    public void set(A first, B second) {
        this.first = first;
        this.second = second;
    }
}

package com.github.wolray.seq;

/**
 * @author wolray
 */
public class Triple<T, A, B> {
    public T first;
    public A second;
    public B third;

    public Triple(T first, A second, B third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    @Override
    public String toString() {
        return String.format("(%s,%s,%s)", first, second, third);
    }
}

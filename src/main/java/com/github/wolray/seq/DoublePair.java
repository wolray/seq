package com.github.wolray.seq;

/**
 * @author wolray
 */
public class DoublePair<T> {
    public T it;
    public double doubleVal;

    public DoublePair(double doubleVal, T it) {
        this.doubleVal = doubleVal;
        this.it = it;
    }

    @Override
    public String toString() {
        return String.format("(%f,%s)", doubleVal, it);
    }
}

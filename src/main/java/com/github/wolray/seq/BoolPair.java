package com.github.wolray.seq;

/**
 * @author wolray
 */
public class BoolPair<T> {
    public T it;
    public boolean flag;

    public BoolPair(boolean flag, T it) {
        this.flag = flag;
        this.it = it;
    }

    @Override
    public String toString() {
        return String.format("(%b,%s)", flag, it);
    }
}

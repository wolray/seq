package com.github.wolray.seq;

/**
 * @author wolray
 */
public class IntPair<T> {
    public T it;
    public int intVal;

    public IntPair(int intVal, T it) {
        this.intVal = intVal;
        this.it = it;
    }

    @Override
    public String toString() {
        return String.format("(%d,%s)", intVal, it);
    }
}

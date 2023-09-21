package com.github.wolray.seq;

/**
 * @author wolray
 */
public class IntDoublePair {
    public int intVal;
    public double doubleVal;

    public IntDoublePair(int intVal, double doubleVal) {
        this.intVal = intVal;
        this.doubleVal = doubleVal;
    }

    @Override
    public String toString() {
        return String.format("(%d,%f)", intVal, doubleVal);
    }
}

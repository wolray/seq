package com.github.wolray.seq;

public final class AverageFolder {
    private double v, w;

    public void accept(double value, double weight) {
        v += value * weight;
        w += weight;
    }

    public Double result() {
        return w != 0 ? v / w : null;
    }
}

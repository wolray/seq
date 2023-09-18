package com.github.wolray.seq;

import java.util.Optional;

/**
 * @author wolray
 */
public class Mutable<T> {
    protected boolean isSet = false;
    protected T it;

    public Mutable(T it) {
        this.it = it;
    }

    public final T get() {
        if (isSet) {
            return it;
        }
        isSet = true;
        return it;
    }

    public T set(T value) {
        isSet = true;
        return this.it = value;
    }

    public Optional<T> toOptional() {
        return isSet ? Optional.ofNullable(it) : Optional.empty();
    }
}

package com.github.wolray.seq;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author wolray
 */
public abstract class PickItr<T> implements Iterator<T> {
    private T next;
    private State state = State.Unset;

    public abstract T pick();

    @Override
    public boolean hasNext() {
        if (state == State.Unset) {
            try {
                next = pick();
                state = State.Cached;
            } catch (NoSuchElementException e) {
                state = State.Done;
            }
        }
        return state == State.Cached;
    }

    @Override
    public T next() {
        if (state == State.Cached) {
            T res = next;
            next = null;
            state = State.Unset;
            return res;
        } else {
            return Seq.stop();
        }
    }

    private enum State {
        Unset,
        Cached,
        Done
    }
}

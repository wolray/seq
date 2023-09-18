package com.github.wolray.seq;

import java.util.Iterator;

/**
 * @author wolray
 */
public abstract class MapItr<T, E> implements Iterator<E> {
    private final Iterator<T> iterator;

    public MapItr(Iterator<T> iterator) {
        this.iterator = iterator;
    }

    public abstract E apply(T t);

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public E next() {
        return apply(iterator.next());
    }
}

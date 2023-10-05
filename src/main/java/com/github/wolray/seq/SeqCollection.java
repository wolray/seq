package com.github.wolray.seq;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author wolray
 */
public interface SeqCollection<T> extends SizedSeq<T>, Collection<T> {
    static <T> SeqCollection<T> of(Collection<T> ts) {
        return ts instanceof SeqCollection ? (SeqCollection<T>)ts : new Proxy<>(ts);
    }

    class Proxy<T, C extends Collection<T>> implements SeqCollection<T> {
        public final C backer;

        public Proxy(C backer) {
            this.backer = backer;
        }

        @Override
        public int size() {
            return backer.size();
        }

        @Override
        public boolean isEmpty() {
            return backer.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return backer.contains(o);
        }

        @Override
        public Object[] toArray() {
            return backer.toArray();
        }

        @Override
        public <E> E[] toArray(E[] a) {
            return backer.toArray(a);
        }

        @Override
        public boolean add(T t) {
            return backer.add(t);
        }

        @Override
        public boolean remove(Object o) {
            return backer.remove(o);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return backer.containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends T> c) {
            return backer.addAll(c);
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return backer.removeAll(c);
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return backer.retainAll(c);
        }

        @Override
        public void clear() {
            backer.clear();
        }

        @Override
        public Iterator<T> iterator() {
            return backer.iterator();
        }
    }
}

package com.github.wolray.seq;

import java.util.*;

/**
 * @author wolray
 */
public interface SeqProxy {
    static <T> SeqCollection<T> ofCollection(Collection<T> collection) {
        return new ProxyCollection<>(collection);
    }

    static <T> SeqList<T> ofList(List<T> list) {
        return new ProxyList<>(list);
    }

    static <T> SeqQueue<T> ofQueue(Queue<T> queue) {
        return new ProxyQueue<>(queue);
    }

    static <T> SeqSet<T> ofSet(Set<T> set) {
        return new ProxySet<>(set);
    }

    class ProxyCollection<T, C extends Collection<T>> implements SeqCollection<T> {
        public final C backer;

        public ProxyCollection(C backer) {
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

    class ProxyList<T> extends ProxyCollection<T, List<T>> implements SeqList<T> {
        public ProxyList(List<T> backer) {
            super(backer);
        }

        @Override
        public boolean addAll(int index, Collection<? extends T> c) {
            return backer.addAll(c);
        }

        @Override
        public T get(int index) {
            return backer.get(index);
        }

        @Override
        public T set(int index, T element) {
            return backer.set(index, element);
        }

        @Override
        public void add(int index, T element) {
            backer.add(index, element);
        }

        @Override
        public T remove(int index) {
            return backer.remove(index);
        }

        @Override
        public int indexOf(Object o) {
            return backer.indexOf(o);
        }

        @Override
        public int lastIndexOf(Object o) {
            return backer.lastIndexOf(o);
        }

        @Override
        public ListIterator<T> listIterator() {
            return backer.listIterator();
        }

        @Override
        public ListIterator<T> listIterator(int index) {
            return backer.listIterator(index);
        }

        @Override
        public List<T> subList(int fromIndex, int toIndex) {
            return backer.subList(fromIndex, toIndex);
        }
    }

    class ProxyQueue<T> extends ProxyCollection<T, Queue<T>> implements SeqQueue<T> {
        public ProxyQueue(Queue<T> backer) {
            super(backer);
        }

        @Override
        public boolean offer(T t) {
            return backer.offer(t);
        }

        @Override
        public T remove() {
            return backer.remove();
        }

        @Override
        public T poll() {
            return backer.poll();
        }

        @Override
        public T element() {
            return backer.element();
        }

        @Override
        public T peek() {
            return backer.peek();
        }
    }

    class ProxySet<T> extends ProxyCollection<T, Set<T>> implements SeqSet<T> {
        public ProxySet(Set<T> backer) {
            super(backer);
        }
    }
}

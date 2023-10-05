package com.github.wolray.seq;

import java.util.Queue;

/**
 * @author wolray
 */
public interface SeqQueue<T> extends SizedSeq<T>, Queue<T> {
    static <T> SeqQueue<T> of(Queue<T> queue) {
        return queue instanceof SeqQueue ? (SeqQueue<T>)queue : new ProxyQueue<>(queue);
    }

    class ProxyQueue<T> extends SeqCollection.Proxy<T, Queue<T>> implements SeqQueue<T> {
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
}

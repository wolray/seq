package com.github.wolray.seq;

import java.util.Set;

/**
 * @author wolray
 */
public interface SeqSet<T> extends SizedSeq<T>, Set<T> {
    static <T> SeqSet<T> of(Set<T> set) {
        return set instanceof SeqSet ? (SeqSet<T>)set : new Proxy<>(set);
    }

    class Proxy<T> extends SeqCollection.Proxy<T, Set<T>> implements SeqSet<T> {
        public Proxy(Set<T> backer) {
            super(backer);
        }
    }
}

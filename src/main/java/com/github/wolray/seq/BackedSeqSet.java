package com.github.wolray.seq;

import java.util.Set;
import java.util.function.Consumer;

/**
 * @author wolray
 */
public class BackedSeqSet<T> extends SeqCollection<T, Set<T>> implements SeqSet<T> {
    BackedSeqSet(Set<T> backer) {
        super(backer);
    }

    @Override
    public void consume(Consumer<T> consumer) {
        forEach(consumer);
    }

    @Override
    public boolean isNotEmpty() {
        return !isEmpty();
    }
}

package com.github.wolray.seq;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * @author wolray
 */
public class HotChannel<T> extends ConcurrentLinkedQueue<T> implements Seq<T>, Async.EasyLock {
    public boolean stop;

    @Override
    public void consume(Consumer<T> consumer) {
        while (true) {
            while (!isEmpty()) {
                consumer.accept(poll());
            }
            easyWait();
        }
    }
}

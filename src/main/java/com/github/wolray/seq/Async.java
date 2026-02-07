package com.github.wolray.seq;

import java.util.concurrent.*;
import java.util.function.Predicate;

/**
 * @author wolray
 */
public interface Async {
    void join(Object task);
    void joinAll(Seq<Runnable> tasks);
    Object submit(Runnable runnable);

    static void apply(ThreadRunnable runnable) {
        try {
            runnable.run();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static Async common() {
        return of(ForkJoinPool.commonPool());
    }

    static Async of(ExecutorService executor) {
        return executor instanceof ForkJoinPool ? of((ForkJoinPool)executor) : new Async() {
            @Override
            public Object submit(Runnable runnable) {
                return CompletableFuture.runAsync(runnable, executor);
            }

            @Override
            public void join(Object task) {
                ((CompletableFuture<?>)task).join();
            }

            @Override
            public void joinAll(Seq<Runnable> tasks) {
                CompletableFuture<?>[] futures = tasks
                    .map(t -> CompletableFuture.runAsync(t, executor))
                    .toObjArray(CompletableFuture[]::new);
                CompletableFuture.allOf(futures).join();
            }
        };
    }

    static Async of(ThreadFactory factory) {
        return new Async() {
            @Override
            public Object submit(Runnable runnable) {
                Thread thread = factory.newThread(runnable);
                thread.start();
                return thread;
            }

            @Override
            public void join(Object task) {
                apply(((Thread)task)::join);
            }

            @Override
            public void joinAll(Seq<Runnable> tasks) {
                SeqList<Runnable> list = tasks.toList();
                CountDownLatch latch = new CountDownLatch(list.size());
                list.consume(r -> factory.newThread(() -> {
                    r.run();
                    latch.countDown();
                }).start());
                apply(latch::wait);
            }
        };
    }

    static Async of(ForkJoinPool forkJoinPool) {
        return new ForkJoin(forkJoinPool);
    }

    static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static <T> Seq<T> sourceOf(Seq<T> seq) {
        return seq instanceof AsyncSeq ? ((AsyncSeq<T>)seq).source : seq;
    }

    default <T> AsyncSeq<T> toAsync(Seq<T> seq) {
        return new AsyncSeq<T>(this, sourceOf(seq)) {
            @Override
            public boolean until(Predicate<T> stop) {
                checkState();
                task = submit(() -> source.until(t -> cancelled || stop.test(t)));
                return false;
            }
        };
    }

    interface ThreadRunnable {
        void run() throws InterruptedException;
    }

    class ForkJoin implements Async {
        final ForkJoinPool forkJoinPool;

        public ForkJoin(ForkJoinPool forkJoinPool) {
            this.forkJoinPool = forkJoinPool;
        }

        @Override
        public Object submit(Runnable runnable) {
            return forkJoinPool.submit(runnable);
        }

        @Override
        public void join(Object task) {
            ((ForkJoinTask<?>)task).join();
        }

        @Override
        public void joinAll(Seq<Runnable> tasks) {
            tasks.map(forkJoinPool::submit).cache().consume(ForkJoinTask::join);
        }
    }
}

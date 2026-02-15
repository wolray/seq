package com.github.wolray.seq;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.function.*;

public interface Downstream<T, E> extends Function<Predicate<E>, Predicate<T>> {
    static <T> Downstream<T, T> distinct() {
        return p -> {
            HashSet<T> set = new HashSet<>();
            return t -> set.add(t) && p.test(t);
        };
    }

    static <T, E> Downstream<T, T> distinctBy(Function<T, E> function) {
        return p -> {
            HashSet<E> set = new HashSet<>();
            return t -> set.add(function.apply(t)) && p.test(t);
        };
    }

    static <T> Downstream<T, T> drop(int n) {
        return p -> new Predicate<T>() {
            int i = 0;

            @Override
            public boolean test(T t) {
                if (i == n) {
                    return p.test(t);
                }
                i++;
                return false;
            }
        };
    }

    static <T> Downstream<T, T> dropWhile(Predicate<T> predicate) {
        return p -> new Predicate<T>() {
            boolean flag = true;

            @Override
            public boolean test(T t) {
                if (flag) {
                    if (predicate.test(t)) {
                        return false;
                    }
                    flag = false;
                }
                return p.test(t);
            }
        };
    }

    static <T> Downstream<T, T> duplicateEach(int times) {
        return p -> t -> {
            for (int i = 0; i < times; i++) {
                if (p.test(t)) {
                    return true;
                }
            }
            return false;
        };
    }

    static <T> Downstream<T, T> duplicateIf(int times, Predicate<T> predicate) {
        return p -> t -> {
            if (predicate.test(t)) {
                for (int i = 0; i < times; i++) {
                    if (p.test(t)) {
                        return true;
                    }
                }
            } else {
                return p.test(t);
            }
            return false;
        };
    }

    static <T> Downstream<T, T> filter(Predicate<T> predicate) {
        return p -> t -> predicate.test(t) && p.test(t);
    }

    static <T> Downstream<T, T> filterIndexed(Seq.IntObjPredicate<T> predicate) {
        return p -> new Predicate<T>() {
            int i = 0;

            @Override
            public boolean test(T t) {
                return predicate.test(i++, t) && p.test(t);
            }
        };
    }

    static <T, E extends T> Downstream<T, E> filterInstance(Class<E> cls) {
        return p -> t -> cls.isInstance(t) && p.test(cls.cast(t));
    }

    static <T> Downstream<T, T> filterNotNull() {
        return p -> t -> t != null && p.test(t);
    }

    static <T, E> Downstream<T, E> flatIterable(Function<T, Iterable<E>> function) {
        return p -> t -> {
            for (E e : function.apply(t)) {
                if (p.test(e)) {
                    return true;
                }
            }
            return false;
        };
    }

    static <T, E> Downstream<T, E> flatMap(Function<T, Seq<E>> function) {
        return p -> t -> function.apply(t).any(p);
    }

    static <T, E> Downstream<T, E> flatOptional(Function<T, Optional<E>> function) {
        return p -> t -> function.apply(t).filter(p).isPresent();
    }

    static <T, E> Downstream<T, E> map(Function<T, E> function) {
        return p -> t -> p.test(function.apply(t));
    }

    static <T, E> Downstream<T, E> mapIf(BiPredicate<Predicate<E>, T> predicate) {
        return p -> t -> predicate.test(p, t);
    }

    static <T, E> Downstream<T, E> mapIndexed(Seq.IntObjFunction<T, E> function) {
        return p -> new Predicate<T>() {
            int i = 0;

            @Override
            public boolean test(T t) {
                return p.test(function.apply(i++, t));
            }
        };
    }

    static <T> Downstream<T, T> onEach(Consumer<T> consumer) {
        return p -> t -> {
            consumer.accept(t);
            return p.test(t);
        };
    }

    static <T> Downstream<T, T> onEachIndexed(Seq.IntObjConsumer<T> consumer) {
        return p -> new Predicate<T>() {
            int i = 0;

            @Override
            public boolean test(T t) {
                consumer.accept(i++, t);
                return p.test(t);
            }
        };
    }

    static <T> Downstream<T, T> replace(int n, UnaryOperator<T> operator) {
        return p -> new Predicate<T>() {
            int i = 1;

            @Override
            public boolean test(T t) {
                if (i > n) {
                    return p.test(t);
                }
                i++;
                return p.test(operator.apply(t));
            }
        };
    }

    static <T, E> Downstream<T, E> runningFold(E init, BiFunction<E, T, E> function) {
        return p -> new Predicate<T>() {
            E cur = init;

            @Override
            public boolean test(T t) {
                return p.test(cur = function.apply(cur, t));
            }
        };
    }

    static <T> Downstream<T, T> take(int n) {
        return p -> new Predicate<T>() {
            int i = 1;

            @Override
            public boolean test(T t) {
                return i++ > n || p.test(t);
            }
        };
    }

    static <T> Downstream<T, T> takeWhile(BiPredicate<T, T> testPrevCurr) {
        return p -> new Predicate<T>() {
            T prev = null;
            boolean first = true;

            @Override
            public boolean test(T t) {
                if (first) {
                    first = false;
                    prev = t;
                    return p.test(t);
                } else {
                    if (testPrevCurr.test(prev, t)) {
                        prev = t;
                        return p.test(t);
                    }
                    return true;
                }
            }
        };
    }

    static <T> Downstream<T, T> takeWhile(Predicate<T> predicate) {
        return p -> t -> !predicate.test(t) || p.test(t);
    }

    static <T> Downstream<T, T> timeLimit(long millis) {
        return p -> {
            long end = System.currentTimeMillis() + millis;
            return t -> System.currentTimeMillis() > end || p.test(t);
        };
    }

    static <T, V> Downstream<T, V> windowedByTime(long timeMillis, Reducer<T, V> reducer) {
        if (timeMillis <= 0) {
            throw new IllegalArgumentException("non-positive time");
        }
        return p -> new Predicate<T>() {
            long last = System.currentTimeMillis();
            Reducer.Worker<T, V> worker = reducer.get();

            @Override
            public boolean test(T t) {
                long now = System.currentTimeMillis();
                if (now - last > timeMillis) {
                    last = now;
                    if (p.test(worker.result())) {
                        return true;
                    }
                    worker = reducer.get();
                }
                return worker.test(t);
            }
        };
    }

    static <T> Downstream<T, IntPair<T>> withIndex() {
        return p -> new Predicate<T>() {
            int i = 0;

            @Override
            public boolean test(T t) {
                return p.test(new IntPair<>(i++, t));
            }
        };
    }

    static <T> Downstream<T, T> zip(T t) {
        return p -> o -> p.test(o) || p.test(t);
    }

    static <T> Staged<T, SeqList<T>> chunked(int size) {
        return chunked(size, Reducer.toList());
    }

    static <T, V> Staged<T, V> chunked(int size, Reducer<T, V> reducer) {
        if (size <= 0) {
            throw new IllegalArgumentException("non-positive size");
        }
        return p -> new StagedPredicate<T>() {
            Reducer.Worker<T, V> worker = reducer.get();
            int idx = 0;

            @Override
            public boolean test(T t) {
                if (idx == size) {
                    if (p.test(worker.result())) {
                        worker = null;
                        return true;
                    }
                    worker = reducer.get();
                    idx = 0;
                }
                worker.test(t);
                idx++;
                return false;
            }

            @Override
            public boolean after() {
                return worker != null && p.test(worker.result());
            }
        };
    }

    static <T> Staged<T, T> union(Iterable<T> iterable) {
        return p -> new StagedPredicate<T>() {
            @Override
            public boolean test(T t) {
                return p.test(t);
            }

            @Override
            public boolean after() {
                for (T t : iterable) {
                    if (p.test(t)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    static <T> Staged<T, T> union(T t) {
        return p -> new StagedPredicate<T>() {
            @Override
            public boolean test(T t) {
                return p.test(t);
            }

            @Override
            public boolean after() {
                return p.test(t);
            }
        };
    }

    static <T, V> Staged<T, V> windowed(int size, int step, boolean allowPartial, Reducer<T, V> reducer) {
        if (size <= 0 || step <= 0) {
            throw new IllegalArgumentException("non-positive size or step");
        }
        return p -> new StagedPredicate<T>() {
            final Queue<IntPair<Reducer.Worker<T, V>>> queue = new LinkedList<>();
            int i = 0;

            @Override
            public boolean test(T t) {
                if (i == 0) {
                    i = step;
                    queue.offer(new IntPair<>(0, reducer.get()));
                }
                for (IntPair<Reducer.Worker<T, V>> sub : queue) {
                    if (sub.it.test(t)) {
                        break;
                    }
                    sub.intVal++;
                }
                IntPair<Reducer.Worker<T, V>> first = queue.peek();
                if (first != null && first.intVal == size) {
                    queue.poll();
                    if (p.test(first.it.result())) {
                        return true;
                    }
                }
                i -= 1;
                return false;
            }

            @Override
            public boolean after() {
                if (allowPartial) {
                    while (!queue.isEmpty()) {
                        if (p.test(queue.poll().it.result())) {
                            queue.clear();
                            return true;
                        }
                    }
                    return false;
                }
                return false;
            }
        };
    }

    interface Staged<T, E> extends Function<Predicate<E>, StagedPredicate<T>> {}

    interface StagedPredicate<T> extends Predicate<T> {
        boolean after();
    }
}

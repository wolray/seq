package com.github.wolray.seq;

import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.*;

/**
 * @author wolray
 */
public interface ItrSeq<T> extends Iterable<T>, Seq<T> {
    @Override
    default void consume(Consumer<T> consumer) {
        for (T t : this) {
            consumer.accept(t);
        }
    }

    @Override
    default ItrSeq<T> asIterable() {
        return this;
    }

    @Override
    default ItrSeq<T> drop(int n) {
        return n <= 0 ? this : () -> {
            Iterator<T> iterator = iterator();
            for (int i = 0; i < n; i++) {
                if (iterator.hasNext()) {
                    iterator.next();
                } else {
                    return Collections.emptyIterator();
                }
            }
            return iterator;
        };
    }

    @Override
    default ItrSeq<T> dropWhile(Predicate<T> predicate) {
        return () -> new Puller<T>() {
            final Iterator<T> iterator = iterator();
            boolean flag = true;

            @Override
            public boolean hasNext() {
                if (iterator.hasNext()) {
                    T t = iterator.next();
                    if (flag) {
                        while (predicate.test(t) && iterator.hasNext()) {
                            t = iterator.next();
                        }
                        flag = false;
                    }
                    next = t;
                    return true;
                }
                return false;
            }
        };
    }

    @Override
    default ItrSeq<T> filter(Predicate<T> predicate) {
        return copyIf(this, (p, t) -> predicate.test(t) && p.set(t));
    }

    @Override
    default ItrSeq<T> filterIndexed(IntObjPredicate<T> predicate) {
        return copyIf(this, (p, t) -> predicate.test(p.index, t) && p.setAndIncrease(t));
    }

    @Override
    default <E extends T> ItrSeq<E> filterInstance(Class<E> cls) {
        return copyIf(this, (p, t) -> cls.isInstance(t) && p.set(cls.cast(t)));
    }

    @Override
    default <E> ItrSeq<E> flatIterable(Function<T, Iterable<E>> function) {
        return () -> flatIterable(map(function));
    }

    @Override
    default <E> ItrSeq<E> flatOptional(Function<T, Optional<E>> function) {
        return Seq.flatOptional(map(function));
    }

    @Override
    default <E> ItrSeq<E> map(Function<T, E> function) {
        return () -> Puller.map(iterator(), function);
    }

    @Override
    default <E> ItrSeq<E> mapIndexed(IntObjFunction<T, E> function) {
        return copyIf(this, (p, t) -> p.setAndIncrease(function.apply(p.index, t)));
    }

    @Override
    default <E> ItrSeq<E> mapMaybe(Function<T, E> function) {
        return copyIf(this, (p, t) -> t != null && p.set(function.apply(t)));
    }

    @Override
    default <E> ItrSeq<E> mapNotNull(Function<T, E> function) {
        return copyIf(this, (p, t) -> {
            E e = function.apply(t);
            return e != null && p.set(e);
        });
    }

    @Override
    default <E> ItrSeq<E> runningFold(E init, BiFunction<E, T, E> function) {
        return () -> {
            Iterator<T> iterator = iterator();
            Puller<E> res = new Puller<E>() {
                @Override
                public boolean hasNext() {
                    if (iterator.hasNext()) {
                        return set(function.apply(next, iterator.next()));
                    }
                    return false;
                }
            };
            res.next = init;
            return res;
        };
    }

    @Override
    default ItrSeq<T> take(int n) {
        return n <= 0 ? ItrSeq.empty() : copyWhile(this, (p, t) -> p.index < n && p.setAndIncrease(t));
    }

    @Override
    default ItrSeq<T> takeWhile(BiPredicate<T, T> testPrevCurr) {
        return copyWhile(this, (p, t) -> (p.index == 0 || testPrevCurr.test(p.next, t)) && p.setAndIncrease(t));
    }

    @Override
    default ItrSeq<T> takeWhile(Predicate<T> predicate) {
        return copyWhile(this, (p, t) -> predicate.test(t) && p.set(t));
    }

    @Override
    default Optional<T> find(Predicate<T> predicate) {
        for (T t : this) {
            if (predicate.test(t)) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }

    @Override
    default T first() {
        for (T t : this) {
            return t;
        }
        return null;
    }

    @Override
    default T last() {
        T res = null;
        for (T t : this) {
            res = t;
        }
        return res;
    }

    @Override
    default boolean until(Predicate<T> stop) {
        for (T t : this) {
            if (stop.test(t)) {
                return true;
            }
        }
        return false;
    }

    static <T, E> ItrSeq<E> copyIf(Iterable<T> iterable, BiPredicate<Puller<E>, T> predicate) {
        return () -> copyIf(iterable.iterator(), predicate);
    }

    static <T, E> ItrSeq<E> copyWhile(Iterable<T> iterable, BiPredicate<Puller<E>, T> predicate) {
        return () -> copyWhile(iterable.iterator(), predicate);
    }

    static <T> ItrSeq<T> empty() {
        return Collections::emptyIterator;
    }

    static <T, E> Puller<E> copyIf(Iterator<T> iterator, BiPredicate<Puller<E>, T> predicate) {
        return new Puller<E>() {
            @Override
            public boolean hasNext() {
                while (iterator.hasNext()) {
                    T t = iterator.next();
                    if (predicate.test(this, t)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    static <T, E> Puller<E> copyWhile(Iterator<T> iterator, BiPredicate<Puller<E>, T> predicate) {
        return new Puller<E>() {
            @Override
            public boolean hasNext() {
                if (iterator.hasNext()) {
                    return predicate.test(this, iterator.next());
                }
                return false;
            }
        };
    }

    static <T> Puller<T> flatIterable(Iterable<? extends Iterable<T>> iterable) {
        return new Puller<T>() {
            final Iterator<? extends Iterable<T>> iterator = iterable.iterator();
            Iterator<T> cur = Collections.emptyIterator();

            @Override
            public boolean hasNext() {
                while (!cur.hasNext()) {
                    if (iterator.hasNext()) {
                        cur = iterator.next().iterator();
                    } else {
                        return false;
                    }
                }
                return pop(cur);
            }
        };
    }

    default <E> void zip(Iterable<E> iterable, BiConsumer<T, E> consumer) {
        Iterator<T> ai = iterator();
        Iterator<E> bi = iterable.iterator();
        while (ai.hasNext() && bi.hasNext()) {
            consumer.accept(ai.next(), bi.next());
        }
    }

    default <E> E fold(E init, BiFunction<E, T, E> function) {
        E acc = init;
        for (T t : this) {
            acc = function.apply(acc, t);
        }
        return acc;
    }

    default <E> ItrSeq<T> takeWhile(Function<T, E> function, BiPredicate<E, E> testPrevCurr) {
        return zipBy(map(function).takeWhile(testPrevCurr), (a, b) -> a);
    }

    default ItrSeq<T> zip(T t) {
        return () -> Puller.zip(iterator(), t);
    }

    default <E, R> ItrSeq<R> zipBy(Iterable<E> iterable, BiFunction<T, E, R> function) {
        return () -> new Puller<R>() {
            final Iterator<T> ti = iterator();
            final Iterator<E> ei = iterable.iterator();

            @Override
            public boolean hasNext() {
                if (ti.hasNext() && ei.hasNext()) {
                    return set(function.apply(ti.next(), ei.next()));
                }
                return false;
            }
        };
    }
}

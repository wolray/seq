package com.github.wolray.seq;

import java.util.*;
import java.util.function.*;

/**
 * @author wolray
 */
@FunctionalInterface
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
        return () -> {
            Iterator<T> iterator = iterator();
            for (int i = 0; i < n; i++) {
                if (iterator.hasNext()) {
                    iterator.next();
                } else {
                    break;
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
        return flatOptional(map(function));
    }

    @Override
    default <E> ItrSeq<E> map(Function<T, E> function) {
        return () -> map(iterator(), function);
    }

    @Override
    default <E> ItrSeq<E> mapIndexed(IntObjFunction<T, E> function) {
        return copyIf(this, (p, t) -> p.setAndIncrease(function.apply(p.index, t)));
    }

    @Override
    default <E> ItrSeq<E> runningFold(E init, BiFunction<E, T, E> function) {
        return () -> new Puller<E>() {
            final Iterator<T> iterator = iterator();

            {
                next = init;
            }

            @Override
            public boolean hasNext() {
                if (iterator.hasNext()) {
                    return set(function.apply(next, iterator.next()));
                }
                return false;
            }
        };
    }

    @Override
    default ItrSeq<T> take(int n) {
        return copyWhile(this, (p, t) -> p.index < n && p.setAndIncrease(t));
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
    default ItrSeq<T> union(Iterable<T> iterable) {
        return flatIterable(this, iterable);
    }

    @Override
    default ItrSeq<T> union(T t) {
        return flatIterable(this, Collections.singletonList(t));
    }

    @Override
    default ItrSeq<T> zip(T t) {
        return () -> zip(iterator(), t);
    }

    @Override
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
    default boolean any(Predicate<T> predicate) {
        for (T t : this) {
            if (predicate.test(t)) {
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

    @SafeVarargs
    static <T> ItrSeq<T> flatIterable(Iterable<T>... iterables) {
        return () -> flatIterable(Arrays.asList(iterables));
    }

    static <T> ItrSeq<T> flatOptional(Iterable<Optional<T>> iterable) {
        return copyIf(iterable, (p, t) -> t.filter(p::set).isPresent());
    }

    static <T> ItrSeq<T> gen(Supplier<T> supplier) {
        return () -> new Puller<T>() {
            @Override
            public boolean hasNext() {
                return set(supplier.get());
            }
        };
    }

    static <T> ItrSeq<T> gen(T seed, UnaryOperator<T> operator) {
        return () -> new Puller<T>() {
            T t = seed;

            @Override
            public boolean hasNext() {
                if (index == 0) {
                    return setAndIncrease(t);
                } else {
                    return set(t = operator.apply(t));
                }
            }
        };
    }

    static <T> ItrSeq<T> gen(T seed1, T seed2, BinaryOperator<T> operator) {
        return () -> new Puller<T>() {
            T t1 = seed1, t2 = seed2;

            @Override
            public boolean hasNext() {
                if (index == 0) {
                    return setAndIncrease(t1);
                } else if (index == 1) {
                    return setAndIncrease(t2);
                } else {
                    return set(t2 = operator.apply(t1, t1 = t2));
                }
            }
        };
    }

    static <T> ItrSeq<T> of(Iterable<T> iterable) {
        if (iterable instanceof ItrSeq) {
            return (ItrSeq<T>)iterable;
        }
        if (iterable instanceof Collection) {
            Collection<T> collection = (Collection<T>)iterable;
            return new SizedSeq<T>() {
                @Override
                public Iterator<T> iterator() {
                    return iterable.iterator();
                }

                @Override
                public int size() {
                    return collection.size();
                }
            };
        }
        return iterable::iterator;
    }

    @SafeVarargs
    static <T> ItrSeq<T> of(T... ts) {
        return of(Arrays.asList(ts));
    }

    static <T> ItrSeq<T> repeat(int n, T t) {
        return () -> new Puller<T>() {
            @Override
            public boolean hasNext() {
                return index < n && setAndIncrease(t);
            }
        };
    }

    static <T> ItrSeq<T> unit(T t) {
        return Collections.singletonList(t)::iterator;
    }

    static <T> ItrSeq<T> untilNull(Supplier<T> supplier) {
        return () -> new Puller<T>() {
            @Override
            public boolean hasNext() {
                T t = supplier.get();
                return t != null && set(t);
            }
        };
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

    static <T, E> Puller<E> map(Iterator<T> iterator, Function<T, E> function) {
        return new Puller<E>() {
            @Override
            public boolean hasNext() {
                if (iterator.hasNext()) {
                    return set(function.apply(iterator.next()));
                }
                return false;
            }
        };
    }

    static <T> Puller<T> zip(Iterator<T> iterator, T t) {
        return new Puller<T>() {
            boolean flag = false;

            @Override
            public boolean hasNext() {
                flag = !flag;
                return flag ? pop(iterator) : set(t);
            }
        };
    }
}

package com.github.wolray.seq;

import java.util.Iterator;
import java.util.Optional;
import java.util.function.*;

/**
 * @author wolray
 */
public interface ItrSeq<T> extends Iterable<T>, Seq<T> {
    @Override
    default ItrSeq<T> asIterable() {
        return this;
    }

    @Override
    default void consume(Consumer<T> consumer) {
        forEach(consumer);
    }

    @Override
    default ItrSeq<T> drop(int n) {
        return () -> ItrUtil.drop(iterator(), n);
    }

    @Override
    default ItrSeq<T> dropWhile(Predicate<T> predicate) {
        return () -> ItrUtil.dropWhile(iterator(), predicate);
    }

    @Override
    default ItrSeq<T> filter(Predicate<T> predicate) {
        return predicate == null ? this : () -> ItrUtil.filter(iterator(), predicate);
    }

    @Override
    default <E> ItrSeq<E> filterInstance(Class<E> cls) {
        return () -> new PickItr<E>() {
            Iterator<T> iterator = iterator();

            @Override
            public E pick() {
                while (iterator.hasNext()) {
                    T t = iterator.next();
                    if (cls.isInstance(t)) {
                        return cls.cast(t);
                    }
                }
                return Seq.stop();
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
    default <E> ItrSeq<E> flatIterable(Function<T, Iterable<E>> function) {
        return () -> ItrUtil.flat(iterator(), function);
    }

    @Override
    default <E> ItrSeq<E> flatOptional(Function<T, Optional<E>> function) {
        return () -> ItrUtil.flatOptional(ItrUtil.map(iterator(), function));
    }

    @Override
    default <E> E fold(E init, BiFunction<E, T, E> function) {
        E acc = init;
        for (T t : this) {
            acc = function.apply(acc, t);
        }
        return acc;
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
    default <E> ItrSeq<E> map(Function<T, E> function) {
        return () -> ItrUtil.map(iterator(), function);
    }

    @Override
    default <E> ItrSeq<E> map(Function<T, E> function, int n, Function<T, E> substitute) {
        return n <= 0 ? map(function) : () -> ItrUtil.map(iterator(), function, n, substitute);
    }

    @Override
    default <E> ItrSeq<E> mapIndexed(IndexObjFunction<T, E> function) {
        return () -> ItrUtil.mapIndexed(iterator(), function);
    }

    @Override
    default <E> ItrSeq<E> mapMaybe(Function<T, E> function) {
        return () -> new PickItr<E>() {
            Iterator<T> iterator = iterator();

            @Override
            public E pick() {
                while (iterator.hasNext()) {
                    T t = iterator.next();
                    if (t != null) {
                        return function.apply(t);
                    }
                }
                return Seq.stop();
            }
        };
    }

    @Override
    default <E> ItrSeq<E> mapNotNull(Function<T, E> function) {
        return () -> new PickItr<E>() {
            Iterator<T> iterator = iterator();

            @Override
            public E pick() {
                while (iterator.hasNext()) {
                    E e = function.apply(iterator.next());
                    if (e != null) {
                        return e;
                    }
                }
                return Seq.stop();
            }
        };
    }

    @Override
    default ItrSeq<T> onEach(Consumer<T> consumer) {
        return map(t -> {
            consumer.accept(t);
            return t;
        });
    }

    @Override
    default ItrSeq<T> onEach(int n, Consumer<T> consumer) {
        return map(t -> t, n, t -> {
            consumer.accept(t);
            return t;
        });
    }

    @Override
    default <E> ItrSeq<E> runningFold(E init, BiFunction<E, T, E> function) {
        return () -> new MapItr<T, E>(iterator()) {
            E acc = init;

            @Override
            public E apply(T t) {
                return acc = function.apply(acc, t);
            }
        };
    }

    @Override
    default ItrSeq<T> take(int n) {
        return () -> ItrUtil.take(iterator(), n);
    }

    @Override
    default ItrSeq<T> takeWhile(Predicate<T> predicate) {
        return () -> ItrUtil.takeWhile(iterator(), predicate);
    }

    @Override
    default <E> ItrSeq<T> takeWhile(Function<T, E> function, BiPredicate<E, E> testPrevCurr) {
        return () -> ItrUtil.takeWhile(iterator(), function, testPrevCurr);
    }

    default ItrSeq<T> zip(T t) {
        return () -> ItrUtil.zip(iterator(), t);
    }
}

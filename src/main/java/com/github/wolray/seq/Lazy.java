package com.github.wolray.seq;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.function.*;

/**
 * @author wolray
 */
public interface Lazy<T> extends Supplier<T> {
    T forkJoin(ForkJoinPool pool);

    static <T> Lazy<T> of(Seq<T> seq) {
        return seq.lazyLast();
    }

    static <T> Lazy<T> of(Supplier<T> supplier) {
        if (supplier instanceof Lazy) {
            return (Lazy<T>)supplier;
        }
        return new Mutable<T>(null) {
            @Override
            protected void eval() {
                it = supplier.get();
            }
        };
    }

    static <A, T> Lazy<T> of(Supplier<A> s1, Function<A, T> function) {
        return of(() -> function.apply(s1.get()));
    }

    static <A, B, T> Lazy<T> of(Supplier<A> s1, Supplier<B> s2, BiFunction<A, B, T> function) {
        return new Mutable<T>(null) {
            @Override
            protected void eval() {
                it = function.apply(s1.get(), s2.get());
            }

            @Override
            protected void eval(ForkJoinPool pool) {
                Supplier<A> a = submit(pool, s1);
                Supplier<B> b = submit(pool, s2);
                it = function.apply(a.get(), b.get());
            }
        };
    }

    static <A, B, C, T> Lazy<T> of(Supplier<A> s1, Supplier<B> s2, Supplier<C> s3, Function3<A, B, C, T> function) {
        return new Mutable<T>(null) {
            @Override
            protected void eval() {
                it = function.apply(s1.get(), s2.get(), s3.get());
            }

            @Override
            protected void eval(ForkJoinPool pool) {
                Supplier<A> u = submit(pool, s1);
                Supplier<B> v = submit(pool, s2);
                Supplier<C> w = submit(pool, s3);
                it = function.apply(u.get(), v.get(), w.get());
            }
        };
    }

    static <A, B, C, D, T> Lazy<T> of(Supplier<A> s1, Supplier<B> s2, Supplier<C> s3, Supplier<D> s4, Function4<A, B, C, D, T> function) {
        return new Mutable<T>(null) {
            @Override
            protected void eval() {
                it = function.apply(s1.get(), s2.get(), s3.get(), s4.get());
            }

            @Override
            protected void eval(ForkJoinPool pool) {
                Supplier<A> a = submit(pool, s1);
                Supplier<B> b = submit(pool, s2);
                Supplier<C> c = submit(pool, s3);
                Supplier<D> d = submit(pool, s4);
                it = function.apply(a.get(), b.get(), c.get(), d.get());
            }
        };
    }

    static <A, B, C, D, E, T> Lazy<T> of(Supplier<A> s1, Supplier<B> s2, Supplier<C> s3, Supplier<D> s4, Supplier<E> s5, Function5<A, B, C, D, E, T> function) {
        return new Mutable<T>(null) {
            @Override
            protected void eval() {
                it = function.apply(s1.get(), s2.get(), s3.get(), s4.get(), s5.get());
            }

            @Override
            protected void eval(ForkJoinPool pool) {
                Supplier<A> a = submit(pool, s1);
                Supplier<B> b = submit(pool, s2);
                Supplier<C> c = submit(pool, s3);
                Supplier<D> d = submit(pool, s4);
                Supplier<E> e = submit(pool, s5);
                it = function.apply(a.get(), b.get(), c.get(), d.get(), e.get());
            }
        };
    }

    static <A, B, C, D, E, F, T> Lazy<T> of(Supplier<A> s1, Supplier<B> s2, Supplier<C> s3, Supplier<D> s4, Supplier<E> s5, Supplier<F> s6, Function6<A, B, C, D, E, F, T> function) {
        return new Mutable<T>(null) {
            @Override
            protected void eval() {
                it = function.apply(s1.get(), s2.get(), s3.get(), s4.get(), s5.get(), s6.get());
            }

            @Override
            protected void eval(ForkJoinPool pool) {
                Supplier<A> a = submit(pool, s1);
                Supplier<B> b = submit(pool, s2);
                Supplier<C> c = submit(pool, s3);
                Supplier<D> d = submit(pool, s4);
                Supplier<E> e = submit(pool, s5);
                Supplier<F> f = submit(pool, s6);
                it = function.apply(a.get(), b.get(), c.get(), d.get(), e.get(), f.get());
            }
        };
    }

    static <T> Supplier<T> submit(ForkJoinPool pool, Supplier<T> supplier) {
        RecursiveTask<T> task;
        if (supplier instanceof Lazy) {
            Lazy<T> lazy = (Lazy<T>)supplier;
            if (lazy.isSet()) {
                return lazy;
            } else {
                task = new RecursiveTask<T>() {
                    @Override
                    protected T compute() {
                        return lazy.forkJoin(pool);
                    }
                };
            }
        } else {
            task = new RecursiveTask<T>() {
                @Override
                protected T compute() {
                    return supplier.get();
                }
            };
        }
        pool.submit(task);
        return task::join;
    }

    static <T> Lazy<T> unset() {
        return of(() -> {
            throw new UnsetException();
        });
    }

    default Lazy<T> andThen(Consumer<T> consumer) {
        return of(() -> {
            T t = get();
            consumer.accept(t);
            return t;
        });
    }

    default T forkJoin() {
        return forkJoin(ForkJoinPool.commonPool());
    }

    default void ifSet(Consumer<T> consumer) {
        if (isSet()) {
            consumer.accept(get());
        }
    }

    default boolean isSet() {
        throw new UnsupportedOperationException();
    }

    default <E> Lazy<E> map(Function<T, E> function) {
        return of(this, function);
    }

    default T set(T value) {
        throw new UnsupportedOperationException();
    }

    default Lazy<T> wrap(UnaryOperator<Supplier<T>> operator) {
        return of(operator.apply(this));
    }

    class UnsetException extends RuntimeException {}
}

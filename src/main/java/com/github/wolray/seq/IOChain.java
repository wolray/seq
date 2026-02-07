package com.github.wolray.seq;

import java.io.*;

/**
 * @author wolray
 */
public interface IOChain<T> {
    T call() throws IOException;

    static <T, E> E apply(T t, Function<T, E> function) {
        try {
            return function.apply(t);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static IOChain<Void> of(Runnable runnable) {
        return () -> {
            runnable.run();
            return null;
        };
    }

    static <T> IOChain<T> of(IOChain<T> supplier) {
        return supplier;
    }

    static <T> IOChain<T> of(T t) {
        return () -> t;
    }

    static IOChain<BufferedReader> ofReader(IOChain<Reader> supplier) {
        return (Closable<BufferedReader>)() -> {
            Reader reader = supplier.call();
            return reader instanceof BufferedReader ? (BufferedReader)reader : new BufferedReader(reader);
        };
    }

    static IOChain<BufferedWriter> ofWriter(IOChain<Writer> supplier) {
        return (Closable<BufferedWriter>)() -> {
            Writer writer = supplier.call();
            return writer instanceof BufferedWriter ? (BufferedWriter)writer : new BufferedWriter(writer);
        };
    }

    default Lazy<T> asLazy() {
        return Lazy.of(this::get);
    }

    default T get() {
        try {
            return call();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    default <E> IOChain<E> map(Function<T, E> function) {
        return () -> function.apply(call());
    }

    default <C extends Closeable> IOChain<C> mapClosable(Function<T, C> function) {
        return (Closable<C>)() -> function.apply(call());
    }

    default IOChain<T> peek(Consumer<T> consumer) {
        return () -> {
            T t = call();
            consumer.accept(t);
            return t;
        };
    }

    default <E> Seq<E> toSeq(Function<T, E> provider) {
        return p -> {
            use(t -> {
                while (true) {
                    E e = provider.apply(t);
                    if (e == null || p.test(e)) {
                        return;
                    }
                }
            });
            return true;
        };
    }

    default void use(Consumer<T> consumer) {
        try {
            consumer.accept(call());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    interface Closable<C extends Closeable> extends IOChain<C> {
        @Override
        default void use(Consumer<C> consumer) {
            try (C closable = call()) {
                consumer.accept(closable);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    interface Consumer<T> {
        void accept(T t) throws IOException;
    }

    interface Function<T, E> {
        E apply(T t) throws IOException;
    }

    interface Runnable {
        void run() throws IOException;
    }
}

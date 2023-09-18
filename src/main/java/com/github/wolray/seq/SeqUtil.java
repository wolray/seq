package com.github.wolray.seq;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author wolray
 */
public interface SeqUtil {
    static <T> void permute(Consumer<List<T>> c, ArrayList<T> list, int i, boolean inplace) {
        int n = list.size();
        if (i == n) {
            c.accept(inplace ? list : new ArrayList<>(list));
            return;
        }
        for (int j = i; j < n; j++) {
            swap(list, i, j);
            permute(c, list, i + 1, inplace);
            swap(list, i, j);
        }
    }

    static <N> void scanTree(Consumer<N> c, int maxDepth, int depth, N node, Function<N, Seq<N>> sub) {
        c.accept(node);
        Seq<N> subSeq = sub.apply(node);
        if (depth < maxDepth) {
            subSeq.consume(n -> {
                if (n != null) {
                    scanTree(c, maxDepth, depth + 1, n, sub);
                }
            });
        }
    }

    static <N> void scanTree(Consumer<N> c, N node, Function<N, Seq<N>> sub) {
        c.accept(node);
        sub.apply(node).consume(n -> {
            if (n != null) {
                scanTree(c, n, sub);
            }
        });
    }

    static <T> Seq<T> seq(Iterable<T> iterable) {
        return Seq.of(iterable);
    }

    static <T> Seq<T> seq(Optional<T> optional) {
        return optional::ifPresent;
    }

    @SafeVarargs
    static <T> Seq<T> seq(T... ts) {
        return new BackedSeq<>(Arrays.asList(ts));
    }

    static <T> Stream<T> stream(Seq<T> seq) {
        Iterator<T> iterator = new Iterator<T>() {
            @Override
            public boolean hasNext() {
                throw new NoSuchElementException();
            }

            @Override
            public T next() {
                throw new NoSuchElementException();
            }

            @Override
            public void forEachRemaining(Consumer<? super T> action) {
                seq.consume(action::accept);
            }
        };
        return ItrUtil.toStream(iterator);
    }

    static <T> void swap(ArrayList<T> list, int i, int j) {
        T t = list.get(i);
        list.set(i, list.get(j));
        list.set(j, t);
    }

    static void toFile(FileWriter fileWriter, Seq<String> seq) {
        try (BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
            seq.consume(s -> {
                try {
                    bufferedWriter.write(s);
                    bufferedWriter.newLine();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}

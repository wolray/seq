package com.github.wolray.seq;

import java.util.HashMap;
import java.util.Map;
import java.util.function.*;

/**
 * @author wolray
 */
public interface SeqExpand<T> extends Function<T, Seq<T>> {
    static <T> SeqExpand<T> of(Function<T, Seq<T>> function) {
        return function instanceof SeqExpand ? (SeqExpand<T>)function : function::apply;
    }

    default SeqExpand<T> filter(Predicate<T> predicate) {
        return t -> apply(t).filter(predicate);
    }

    default SeqExpand<T> filterNot(Predicate<T> predicate) {
        return t -> apply(t).filter(predicate.negate());
    }

    default void scan(BiConsumer<T, ArraySeq<T>> c, T node) {
        ArraySeq<T> sub = apply(node).filterNotNull().toList();
        c.accept(node, sub);
        sub.consume(n -> scan(c, n));
    }

    default void scan(Consumer<T> c, T node) {
        c.accept(node);
        apply(node).consume(n -> {
            if (n != null) {
                scan(c, n);
            }
        });
    }

    default void scan(Consumer<T> c, T node, int maxDepth, int depth) {
        c.accept(node);
        if (depth < maxDepth) {
            apply(node).consume(n -> {
                if (n != null) {
                    scan(c, n, maxDepth, depth + 1);
                }
            });
        }
    }

    default SeqExpand<T> terminate(Predicate<T> predicate) {
        return t -> predicate.test(t) ? Seq.empty() : apply(t);
    }

    default Map<T, ArraySeq<T>> toDAG(Seq<T> nodes) {
        return nodes.reduce(new HashMap<>(), (map, node) -> scan(map::put, node));
    }

    default Map<T, ArraySeq<T>> toDAG(T node) {
        Map<T, ArraySeq<T>> map = new HashMap<>();
        scan(map::put, node);
        return map;
    }

    default Seq<T> toSeq(T node) {
        return c -> scan(c, node);
    }

    default Seq<T> toSeq(T node, int maxDepth) {
        return c -> scan(c, node, maxDepth, 0);
    }
}
package com.github.wolray.seq;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * @author wolray
 */
public interface SeqExpand<T> extends Function<T, Seq<T>> {
    default SeqExpand<T> filter(Predicate<T> predicate) {
        return t -> apply(t).filter(predicate);
    }

    default SeqExpand<T> filterNot(Predicate<T> predicate) {
        return t -> apply(t).filter(predicate.negate());
    }

    default SeqExpand<T> mapping(UnaryOperator<Seq<T>> operator) {
        return t -> operator.apply(apply(t));
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

    default void scan(Map<T, ArraySeq<T>> map, T node) {
        if (map.containsKey(node)) {
            return;
        }
        ArraySeq<T> sub = apply(node).filterNotNull().toList();
        map.put(node, sub);
        sub.forEach(n -> scan(map, n));
    }

    default SeqExpand<T> terminate(Predicate<T> predicate) {
        return t -> predicate.test(t) ? Seq.empty() : apply(t);
    }

    default Map<T, ArraySeq<T>> toDAG(Seq<T> nodes) {
        return nodes.reduce(new HashMap<>(), this::scan);
    }

    default Map<T, ArraySeq<T>> toDAG(T node) {
        Map<T, ArraySeq<T>> map = new HashMap<>();
        scan(map, node);
        return map;
    }

    default Seq<T> toSeq(T node) {
        return c -> scan(c, node);
    }

    default Seq<T> toSeq(T node, int maxDepth) {
        return c -> scan(c, node, maxDepth, 0);
    }
}
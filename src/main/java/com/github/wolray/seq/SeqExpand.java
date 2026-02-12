package com.github.wolray.seq;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author wolray
 */
public interface SeqExpand<T> extends Function<T, Seq<T>> {
    static <T> SeqExpand<T> of(Function<T, Seq<T>> function) {
        return function instanceof SeqExpand ? (SeqExpand<T>)function : function::apply;
    }

    default Map<T, SeqList<T>> toDAG(Seq<T> nodes) {
        Map<T, SeqList<T>> map = new HashMap<>();
        SeqExpand<T> expand = terminate(map::containsKey);
        nodes.until(t -> expand.scan((x, ls) -> {
            map.putIfAbsent(x, ls);
            return false;
        }, t));
        return map;
    }

    default Map<T, SeqList<T>> toDAG(T node) {
        Map<T, SeqList<T>> map = new HashMap<>();
        terminate(map::containsKey).scan((t, ls) -> {
            map.putIfAbsent(t, ls);
            return false;
        }, node);
        return map;
    }

    default Seq<T> toSeq(T node) {
        return p -> scan(p, node);
    }

    default Seq<T> toSeq(T node, int maxDepth) {
        return p -> scan(p, node, maxDepth, 0);
    }

    default SeqExpand<T> filter(Predicate<T> predicate) {
        return t -> apply(t).filter(predicate);
    }

    default SeqExpand<T> filterNot(Predicate<T> predicate) {
        return t -> apply(t).filter(predicate.negate());
    }

    default SeqExpand<T> terminate(Predicate<T> predicate) {
        return t -> predicate.test(t) ? Seq.empty() : apply(t);
    }

    default boolean scan(BiPredicate<T, SeqList<T>> p, T node) {
        SeqList<T> sub = apply(node).filterNotNull().toList();
        return p.test(node, sub) || sub.until(n -> scan(p, n));
    }

    default boolean scan(Predicate<T> p, T node) {
        return p.test(node) || apply(node).until(n -> n != null && scan(p, n));
    }

    default boolean scan(Predicate<T> p, T node, int maxDepth, int depth) {
        if (p.test(node)) {
            return true;
        }
        if (depth < maxDepth) {
            return apply(node).until(n -> n != null && scan(p, n, maxDepth, depth + 1));
        }
        return false;
    }
}

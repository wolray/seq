package com.github.wolray.seq;

import java.util.function.Function;

/**
 * @author wolray
 */
public interface Seq3<A, B, C> extends Seq0<Consumer3<A, B, C>> {
    @SuppressWarnings("unchecked")
    static <A, B, C> Seq3<A, B, C> empty() {
        return (Seq3<A, B, C>)Empty.emptySeq;
    }

    @SuppressWarnings("unchecked")
    static <A, B, C> Consumer3<A, B, C> nothing() {
        return (Consumer3<A, B, C>)Empty.nothing;
    }

    default Seq3<A, B, C> filter(TriPredicate<A, B, C> predicate) {
        return cs -> consume((a, b, c) -> {
            if (predicate.test(a, b, c)) {
                cs.accept(a, b, c);
            }
        });
    }

    default Triple<A, B, C> first() {
        Triple<A, B, C> t = new Triple<>(null, null, null);
        consumeTillStop((a, b, c) -> {
            t.first = a;
            t.second = b;
            t.third = c;
            Seq.stop();
        });
        return t;
    }

    default Seq<A> keepFirst() {
        return cs -> consume((a, b, c) -> cs.accept(a));
    }

    default Seq<B> keepSecond() {
        return cs -> consume((a, b, c) -> cs.accept(b));
    }

    default Seq<C> keepThird() {
        return cs -> consume((a, b, c) -> cs.accept(c));
    }

    default <T> Seq<T> map(Function3<A, B, C, T> function3) {
        return cs -> consume((a, b, c) -> cs.accept(function3.apply(a, b, c)));
    }

    default <T> Seq3<T, B, C> mapFirst(Function3<A, B, C, T> function) {
        return cs -> consume((a, b, c) -> cs.accept(function.apply(a, b, c), b, c));
    }

    default <T> Seq3<T, B, C> mapFirst(Function<A, T> function) {
        return cs -> consume((a, b, c) -> cs.accept(function.apply(a), b, c));
    }

    default <T> Seq3<A, T, C> mapSecond(Function<B, T> function) {
        return cs -> consume((a, b, c) -> cs.accept(a, function.apply(b), c));
    }

    default <T> Seq3<A, T, C> mapSecond(Function3<A, B, C, T> function3) {
        return cs -> consume((a, b, c) -> cs.accept(a, function3.apply(a, b, c), c));
    }

    default <T> Seq3<A, B, T> mapThird(Function<C, T> function) {
        return cs -> consume((a, b, c) -> cs.accept(a, b, function.apply(c)));
    }

    default <T> Seq3<A, B, T> mapThird(Function3<A, B, C, T> function3) {
        return cs -> consume((a, b, c) -> cs.accept(a, b, function3.apply(a, b, c)));
    }

    default Seq<Triple<A, B, C>> tripled() {
        return map(Triple::new);
    }

    interface TriPredicate<A, B, D> {
        boolean test(A a, B b, D d);
    }

    class Empty {
        static final Seq3<Object, Object, Object> emptySeq = cs -> {};
        static final Consumer3<Object, Object, Object> nothing = (a, b, c) -> {};
    }
}

package com.github.wolray.seq;

import java.util.function.*;

@FunctionalInterface
public interface IntSeq {
    boolean any(IntPredicate predicate);

    static IntSeq of(CharSequence cs) {
        return p -> {
            for (int i = 0; i < cs.length(); i++) {
                if (p.test(cs.charAt(i))) {
                    return true;
                }
            }
            return false;
        };
    }

    static IntSeq of(char[] chars) {
        return p -> {
            for (char c : chars) {
                if (p.test(c)) {
                    return true;
                }
            }
            return false;
        };
    }

    static IntSeq of(int... ts) {
        return p -> {
            for (int t : ts) {
                if (p.test(t)) {
                    return true;
                }
            }
            return false;
        };
    }

    static IntSeq range(int stop) {
        return range(0, stop, 1);
    }

    static IntSeq range(int start, int stop) {
        return range(start, stop, 1);
    }

    static IntSeq range(int start, int stop, int step) {
        if (step == 0) {
            throw new IllegalArgumentException("step is 0");
        }
        return p -> {
            if (step > 0) {
                for (int i = start; i < stop; i += step) {
                    if (p.test(i)) {
                        return true;
                    }
                }
            } else {
                for (int i = start; i > stop; i += step) {
                    if (p.test(i)) {
                        return true;
                    }
                }
            }
            return false;
        };
    }

    default IntSeq filter(IntPredicate predicate) {
        return p -> any(i -> predicate.test(i) && p.test(i));
    }

    default IntSeq map(IntUnaryOperator operator) {
        return p -> any(i -> p.test(operator.applyAsInt(i)));
    }

    default Integer max() {
        return reduce(new IntReducer<Integer>() {
            Integer v = null;

            @Override
            public boolean test(int value) {
                if (v == null || v < value) {
                    v = value;
                }
                return false;
            }

            @Override
            public Integer result() {
                return v;
            }
        });
    }

    default Integer min() {
        return reduce(new IntReducer<Integer>() {
            Integer v = null;

            @Override
            public boolean test(int value) {
                if (v == null || v > value) {
                    v = value;
                }
                return false;
            }

            @Override
            public Integer result() {
                return v;
            }
        });
    }

    default Seq<Integer> boxed() {
        return p -> any(p::test);
    }

    default <T> Seq<T> downstream(Function<Predicate<T>, IntPredicate> function) {
        return p -> any(function.apply(p));
    }

    default <T> Seq<T> mapToObj(IntFunction<T> function) {
        return p -> any(i -> p.test(function.apply(i)));
    }

    default <K, V> Seq2<K, V> downstream2(Function<BiPredicate<K, V>, IntPredicate> function) {
        return p -> any(function.apply(p));
    }

    default <V> V reduce(IntReducer<V> reducer) {
        any(reducer);
        return reducer.result();
    }

    default int sum() {
        int[] s = new int[1];
        any(i -> {
            s[0] += i;
            return false;
        });
        return s[0];
    }
}

package com.github.wolray.seq;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.OptionalInt;
import java.util.function.*;

/**
 * @author wolray
 */
public interface IntSeq extends Seq0<IntConsumer> {
    IntSeq empty = c -> {};
    IntConsumer nothing = t -> {};

    static IntSeq gen(IntSupplier supplier) {
        return c -> {
            while (true) {
                c.accept(supplier.getAsInt());
            }
        };
    }

    static IntSeq gen(int seed, IntUnaryOperator operator) {
        return c -> {
            int t = seed;
            c.accept(t);
            while (true) {
                c.accept(t = operator.applyAsInt(t));
            }
        };
    }

    static IntSeq gen(int seed1, int seed2, IntBinaryOperator operator) {
        return c -> {
            int t1 = seed1, t2 = seed2;
            c.accept(t1);
            c.accept(t2);
            while (true) {
                c.accept(t2 = operator.applyAsInt(t1, t1 = t2));
            }
        };
    }

    static IntSeq of(CharSequence cs) {
        return c -> {
            for (int i = 0; i < cs.length(); i++) {
                c.accept(cs.charAt(i));
            }
        };
    }

    static IntSeq of(int... ts) {
        return c -> {
            for (int t : ts) {
                c.accept(t);
            }
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
        return c -> {
            if (step > 0) {
                for (int i = start; i < stop; i += step) {
                    c.accept(i);
                }
            } else {
                for (int i = start; i > stop; i += step) {
                    c.accept(i);
                }
            }
        };
    }

    static IntSeq repeat(int n, int value) {
        return c -> {
            for (int i = 0; i < n; i++) {
                c.accept(value);
            }
        };
    }

    default boolean all(IntPredicate predicate) {
        return !find(predicate.negate()).isPresent();
    }

    default boolean any(IntPredicate predicate) {
        return find(predicate).isPresent();
    }

    default boolean anyNot(IntPredicate predicate) {
        return any(predicate.negate());
    }

    default IntSeq append(int t) {
        return c -> {
            consume(c);
            c.accept(t);
        };
    }

    default IntSeq append(int... t) {
        return c -> {
            consume(c);
            for (int x : t) {
                c.accept(x);
            }
        };
    }

    default IntSeq appendWith(IntSeq seq) {
        return c -> {
            consume(c);
            seq.consume(c);
        };
    }

    default double average() {
        return average(null);
    }

    default double average(IntToDoubleFunction weightFunction) {
        double[] a = {0, 0};
        consume(t -> {
            if (weightFunction != null) {
                double w = weightFunction.applyAsDouble(t);
                a[0] += t * w;
                a[1] += w;
            } else {
                a[0] += t;
                a[1] += 1;
            }
        });
        return a[1] != 0 ? a[0] / a[1] : 0;
    }

    default Seq<Integer> boxed() {
        return c -> consume(c::accept);
    }

    default IntSeq circle() {
        return c -> {
            while (true) {
                consume(c);
            }
        };
    }

    default void consume(IntConsumer consumer, int n, IntConsumer substitute) {
        if (n > 0) {
            int[] a = {n - 1};
            consume(t -> {
                if (a[0] < 0) {
                    consumer.accept(t);
                } else {
                    a[0]--;
                    substitute.accept(t);
                }
            });
        } else {
            consume(consumer);
        }
    }

    default void consumeIndexed(IndexIntConsumer consumer) {
        int[] a = {0};
        consume(t -> consumer.accept(a[0]++, t));
    }

    default void consumeIndexedTillStop(IndexIntConsumer consumer) {
        int[] a = {0};
        consumeTillStop(t -> consumer.accept(a[0]++, t));
    }

    default int count() {
        return reduce(new int[1], (a, t) -> a[0]++)[0];
    }

    default int count(IntPredicate predicate) {
        return reduce(new int[1], (a, t) -> {
            if (predicate.test(t)) {
                a[0]++;
            }
        })[0];
    }

    default int countNot(IntPredicate predicate) {
        return count(predicate.negate());
    }

    default IntSeq distinct() {
        return distinctBy(i -> i);
    }

    default <E> IntSeq distinctBy(IntFunction<E> function) {
        return c -> reduce(new HashSet<>(), (set, t) -> {
            if (set.add(function.apply(t))) {
                c.accept(t);
            }
        });
    }

    default IntSeq drop(int n) {
        return n <= 0 ? this : partial(n, nothing);
    }

    default IntSeq dropWhile(IntPredicate predicate) {
        return c -> foldBoolean(false, (b, t) -> {
            if (b || !predicate.test(t)) {
                c.accept(t);
                return true;
            }
            return false;
        });
    }

    default IntSeq duplicateAll(int times) {
        return c -> {
            for (int i = 0; i < times; i++) {
                consume(c);
            }
        };
    }

    default IntSeq duplicateEach(int times) {
        return c -> consume(t -> {
            for (int i = 0; i < times; i++) {
                c.accept(t);
            }
        });
    }

    default IntSeq duplicateIf(int times, IntPredicate predicate) {
        return c -> consume(t -> {
            if (predicate.test(t)) {
                for (int i = 0; i < times; i++) {
                    c.accept(t);
                }
            } else {
                c.accept(t);
            }
        });
    }

    default IntSeq filter(IntPredicate predicate) {
        return c -> consume(t -> {
            if (predicate.test(t)) {
                c.accept(t);
            }
        });
    }

    default IntSeq filter(int n, IntPredicate predicate) {
        return c -> consume(c, n, t -> {
            if (predicate.test(t)) {
                c.accept(t);
            }
        });
    }

    default IntSeq filterIndexed(IndexIntPredicate predicate) {
        return c -> consumeIndexed((i, t) -> {
            if (predicate.test(i, t)) {
                c.accept(t);
            }
        });
    }

    default IntSeq filterNot(IntPredicate predicate) {
        return filter(predicate.negate());
    }

    default OptionalInt find(IntPredicate predicate) {
        Mutable<Integer> m = new Mutable<>(null);
        consumeTillStop(t -> {
            if (predicate.test(t)) {
                m.set(t);
                Seq.stop();
            }
        });
        return m.isSet ? OptionalInt.of(m.it) : OptionalInt.empty();
    }

    default OptionalInt findNot(IntPredicate predicate) {
        return find(predicate.negate());
    }

    default OptionalInt first() {
        return find(t -> true);
    }

    default IntSeq flatMap(IntFunction<IntSeq> function) {
        return c -> consume(t -> function.apply(t).consume(c));
    }

    default <E> E fold(E init, ObjIntToObj<E> function) {
        Mutable<E> m = new Mutable<>(init);
        consume(t -> m.it = function.apply(m.it, t));
        return m.it;
    }

    default int foldInt(int init, IntBinaryOperator function) {
        int[] a = {init};
        consume(i -> a[0] = function.applyAsInt(a[0], i));
        return a[0];
    }

    default double foldDouble(double init, DoubleIntToDouble function) {
        double[] a = {init};
        consume(i -> a[0] = function.apply(a[0], i));
        return a[0];
    }

    default long foldLong(long init, LongIntToLong function) {
        long[] a = {init};
        consume(i -> a[0] = function.apply(a[0], i));
        return a[0];
    }

    default boolean foldBoolean(boolean init, BoolIntToBool function) {
        boolean[] a = {init};
        consume(i -> a[0] = function.apply(a[0], i));
        return a[0];
    }

    default OptionalInt last() {
        Mutable<Integer> m = new Mutable<>(null);
        consume(m::set);
        return m.isSet ? OptionalInt.of(m.it) : OptionalInt.empty();
    }

    default OptionalInt last(IntPredicate predicate) {
        return filter(predicate).last();
    }

    default OptionalInt lastNot(IntPredicate predicate) {
        return last(predicate.negate());
    }

    default IntSeq map(IntUnaryOperator function) {
        return c -> consume(t -> c.accept(function.applyAsInt(t)));
    }

    default IntSeq mapIndexed(IndexIntToInt function) {
        return c -> consumeIndexed((i, t) -> c.accept(function.apply(i, t)));
    }

    default <E> Seq<E> mapToObj(IntFunction<E> function) {
        return c -> consume(t -> c.accept(function.apply(t)));
    }

    default <E> Seq<E> mapToObj(IntFunction<E> function, int n, IntFunction<E> substitute) {
        return n <= 0 ? mapToObj(function) : c -> {
            int[] a = {n - 1};
            consume(t -> {
                if (a[0] < 0) {
                    c.accept(function.apply(t));
                } else {
                    a[0]--;
                    c.accept(substitute.apply(t));
                }
            });
        };
    }

    default Integer max() {
        return fold(null, (f, t) -> f == null || f < t ? t : f);
    }

    default <V extends Comparable<V>> IntPair<V> max(IntFunction<V> function) {
        return reduce(new IntPair<>(0, null), (p, t) -> {
            V v = function.apply(t);
            if (p.it == null || p.it.compareTo(v) < 0) {
                p.intVal = t;
                p.it = v;
            }
        });
    }

    default Integer min() {
        return fold(null, (f, t) -> f == null || f > t ? t : f);
    }

    default <V extends Comparable<V>> IntPair<V> min(IntFunction<V> function) {
        return reduce(new IntPair<>(0, null), (p, t) -> {
            V v = function.apply(t);
            if (p.it == null || p.it.compareTo(v) > 0) {
                p.intVal = t;
                p.it = v;
            }
        });
    }

    default boolean none(IntPredicate predicate) {
        return !find(predicate).isPresent();
    }

    default IntSeq onEach(IntConsumer consumer) {
        return c -> consume(consumer.andThen(c));
    }

    default IntSeq onEach(int n, IntConsumer consumer) {
        return c -> consume(c, n, consumer.andThen(c));
    }

    default IntSeq onEachIndexed(IndexIntConsumer consumer) {
        return c -> consumeIndexed((i, t) -> {
            consumer.accept(i, t);
            c.accept(t);
        });
    }

    default IntSeq partial(int n, IntConsumer substitute) {
        return c -> consume(c, n, substitute);
    }

    default <E> E reduce(E des, ObjIntConsumer<E> consumer) {
        consume(t -> consumer.accept(des, t));
        return des;
    }

    default IntSeq replace(int n, IntUnaryOperator operator) {
        return c -> consume(c, n, t -> c.accept(operator.applyAsInt(t)));
    }

    default IntSeq runningFold(int init, IntBinaryOperator function) {
        return c -> foldInt(init, (acc, t) -> {
            acc = function.applyAsInt(acc, t);
            c.accept(acc);
            return acc;
        });
    }

    default int sum() {
        return reduce(new int[1], (a, t) -> a[0] += t)[0];
    }

    default int sum(IntUnaryOperator function) {
        return reduce(new int[1], (a, t) -> a[0] += function.applyAsInt(t))[0];
    }

    default IntSeq take(int n) {
        return n <= 0 ? empty : c -> {
            int[] i = {n};
            consumeTillStop(t -> {
                if (i[0]-- > 0) {
                    c.accept(t);
                } else {
                    Seq.stop();
                }
            });
        };
    }

    default IntSeq takeWhile(IntPredicate predicate) {
        return c -> consumeTillStop(t -> {
            if (predicate.test(t)) {
                c.accept(t);
            } else {
                Seq.stop();
            }
        });
    }

    default int[] toArray() {
        return toBatched().toArray();
    }

    default Batched toBatched() {
        return reduce(new Batched(), Batched::add);
    }

    interface ObjIntConsumer<E> {
        void accept(E e, int i);
    }

    interface ObjIntToObj<E> {
        E apply(E e, int t);
    }

    interface DoubleIntToDouble {
        long apply(double acc, int t);
    }

    interface LongIntToLong {
        long apply(long acc, int t);
    }

    interface BoolIntToBool {
        boolean apply(boolean acc, int t);
    }

    interface IndexIntConsumer {
        void accept(int i, int t);
    }

    interface IndexIntPredicate {
        boolean test(int i, int t);
    }

    interface IndexIntToInt {
        int apply(int i, int t);
    }

    class Batched implements IntSeq {
        private final LinkedList<int[]> list = new LinkedList<>();
        private int batchSize = 10;
        public int size;
        private int[] cur;
        private int index;

        @Override
        public void consume(IntConsumer consumer) {
            list.forEach(a -> {
                for (int i = 0, size = sizeOf(a); i < size; i++) {
                    consumer.accept(a[i]);
                }
            });
        }

        @Override
        public int[] toArray() {
            int[] a = new int[size];
            int pos = 0;
            for (int[] sub : list) {
                System.arraycopy(sub, 0, a, pos, sizeOf(sub));
                pos += sub.length;
            }
            return a;
        }

        public void add(int t) {
            if (cur == null) {
                cur = new int[batchSize];
                list.add(cur);
                index = 0;
            }
            cur[index++] = t;
            size++;
            if (index == batchSize) {
                cur = null;
                batchSize = Math.min(300, Math.max(batchSize, size >> 1));
            }
        }

        private int sizeOf(int[] a) {
            return a != cur ? a.length : index;
        }
    }
}

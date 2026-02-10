package com.github.wolray.seq;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;

/**
 * @author wolray
 */
public interface Reducer<T, V> {
    Worker<T, V> get();

    static <T> Reducer<T, Double> average(ToDoubleFunction<T> function) {
        return average(function, t -> 1);
    }

    static <T> Reducer<T, Double> average(ToDoubleFunction<T> function, ToDoubleFunction<T> weightFunction) {
        return () -> new Worker<T, Double>() {
            double v = 0, w = 0;

            @Override
            public void accept(T t) {
                double wt = weightFunction.applyAsDouble(t);
                v += function.applyAsDouble(t) * wt;
                w += wt;
            }

            @Override
            public Double result() {
                return w != 0 ? v / w : 0;
            }
        };
    }

    static <T, C extends Collection<T>> Reducer<T, C> collect(Supplier<C> des) {
        return of(des, Collection::add);
    }

    static <T> Reducer<T, Integer> count() {
        return () -> new Worker<T, Integer>() {
            int cnt = 0;

            @Override
            public void accept(T t) {
                cnt++;
            }

            @Override
            public Integer result() {
                return cnt;
            }
        };
    }

    static <T> Reducer<T, Integer> count(Predicate<T> predicate) {
        return () -> new Worker<T, Integer>() {
            int cnt = 0;

            @Override
            public void accept(T t) {
                if (predicate.test(t)) {
                    cnt++;
                }
            }

            @Override
            public Integer result() {
                return cnt;
            }
        };
    }

    static <T> Reducer<T, Integer> countNot(Predicate<T> predicate) {
        return count(predicate.negate());
    }

    static <T, V> Reducer<T, V> filtering(Predicate<T> predicate, Reducer<T, V> reducer) {
        return () -> new Worker<T, V>() {
            final Worker<T, V> worker = reducer.get();

            @Override
            public void accept(T t) {
                if (predicate.test(t)) {
                    worker.accept(t);
                }
            }

            @Override
            public V result() {
                return worker.result();
            }
        };
    }

    static <T> Reducer<T, Optional<T>> find(Predicate<T> predicate) {
        return () -> new Worker<T, Optional<T>>() {
            boolean isSet;
            T value;

            @Override
            public void accept(T t) {
                if (predicate.test(t)) {
                    isSet = true;
                    value = t;
                }
            }

            @Override
            public Optional<T> result() {
                return isSet ? Optional.ofNullable(value) : Optional.empty();
            }
        };
    }

    static <T> Reducer<T, T> first() {
        return () -> new Worker<T, T>() {
            boolean flag = true;
            T value = null;

            @Override
            public void accept(T t) {
                if (flag) {
                    value = t;
                    flag = false;
                }
            }

            @Override
            public T result() {
                return value;
            }
        };
    }

    static <T> Reducer<T, T> fold(BinaryOperator<T> operator) {
        return () -> new Worker<T, T>() {
            T cur = null;

            @Override
            public void accept(T t) {
                cur = cur == null ? t : operator.apply(cur, t);
            }

            @Override
            public T result() {
                return cur;
            }
        };
    }

    static <T> Reducer<T, T> fold(T seed, BinaryOperator<T> operator) {
        return () -> new Worker<T, T>() {
            T cur = seed;

            @Override
            public void accept(T t) {
                cur = operator.apply(cur, t);
            }

            @Override
            public T result() {
                return cur;
            }
        };
    }

    static <T, K, V> Reducer<T, SeqMap<K, V>> groupBy(Function<T, K> toKey, Reducer<T, V> reducer) {
        return () -> new Worker<T, SeqMap<K, V>>() {
            final SeqMap<K, Worker<T, V>> map = new SeqMap<>();

            @Override
            public void accept(T t) {
                map.computeIfAbsent(toKey.apply(t), k -> reducer.get()).accept(t);
            }

            @Override
            public SeqMap<K, V> result() {
                return map.mapValues(Worker::result);
            }
        };
    }

    static Reducer<String, String> join(String sep) {
        return () -> new Worker<String, String>() {
            final StringJoiner joiner = new StringJoiner(sep);

            @Override
            public void accept(String t) {
                joiner.add(t);
            }

            @Override
            public String result() {
                return joiner.toString();
            }
        };
    }

    static <T> Reducer<T, String> join(String sep, Function<T, String> function) {
        return () -> new Worker<T, String>() {
            final StringJoiner joiner = new StringJoiner(sep);

            @Override
            public void accept(T t) {
                joiner.add(function.apply(t));
            }

            @Override
            public String result() {
                return joiner.toString();
            }
        };
    }

    static <T> Reducer<T, T> last() {
        return () -> new Worker<T, T>() {
            T value = null;

            @Override
            public void accept(T t) {
                value = t;
            }

            @Override
            public T result() {
                return value;
            }
        };
    }

    static <T, E> Reducer<T, SeqList<E>> mapping(Function<T, E> mapper) {
        return mapping(mapper, toList());
    }

    static <T, E, V> Reducer<T, V> mapping(Function<T, E> mapper, Reducer<E, V> reducer) {
        return () -> new Worker<T, V>() {
            final Worker<E, V> worker = reducer.get();

            @Override
            public void accept(T t) {
                worker.accept(mapper.apply(t));
            }

            @Override
            public V result() {
                return worker.result();
            }
        };
    }

    static <T, V, E> Reducer<T, E> mapping(Reducer<T, V> reducer, Function<V, E> mapper) {
        return () -> new Worker<T, E>() {
            final Worker<T, V> worker = reducer.get();

            @Override
            public void accept(T t) {
                worker.accept(t);
            }

            @Override
            public E result() {
                return mapper.apply(worker.result());
            }
        };
    }

    static <T> Reducer<T, T> max(Comparator<T> comparator) {
        return () -> new Worker<T, T>() {
            T max = null;

            @Override
            public void accept(T t) {
                if (max == null || comparator.compare(max, t) < 0) {
                    max = t;
                }
            }

            @Override
            public T result() {
                return max;
            }
        };
    }

    static <T, V extends Comparable<V>> Reducer<T, Pair<T, V>> maxBy(Function<T, V> function) {
        return () -> new Worker<T, Pair<T, V>>() {
            T max = null;
            V val = null;

            @Override
            public void accept(T t) {
                V v = function.apply(t);
                if (val == null || val.compareTo(v) < 0) {
                    max = t;
                    val = v;
                }
            }

            @Override
            public Pair<T, V> result() {
                return new Pair<>(max, val);
            }
        };
    }

    static <T> Reducer<T, IntPair<T>> maxByInt(ToIntFunction<T> function) {
        return () -> new Worker<T, IntPair<T>>() {
            T max = null;
            int val = 0;

            @Override
            public void accept(T t) {
                int v = function.applyAsInt(t);
                if (max == null || val < v) {
                    max = t;
                    val = v;
                }
            }

            @Override
            public IntPair<T> result() {
                return new IntPair<>(val, max);
            }
        };
    }

    static <T> Reducer<T, DoublePair<T>> maxByDouble(ToDoubleFunction<T> function) {
        return () -> new Worker<T, DoublePair<T>>() {
            T max = null;
            double val = 0;

            @Override
            public void accept(T t) {
                double v = function.applyAsDouble(t);
                if (max == null || val < v) {
                    max = t;
                    val = v;
                }
            }

            @Override
            public DoublePair<T> result() {
                return new DoublePair<>(val, max);
            }
        };
    }

    static <T> Reducer<T, LongPair<T>> maxByLong(ToLongFunction<T> function) {
        return () -> new Worker<T, LongPair<T>>() {
            T max = null;
            long val = 0;

            @Override
            public void accept(T t) {
                long v = function.applyAsLong(t);
                if (max == null || val < v) {
                    max = t;
                    val = v;
                }
            }

            @Override
            public LongPair<T> result() {
                return new LongPair<>(val, max);
            }
        };
    }

    static <T> Reducer<T, T> min(Comparator<T> comparator) {
        return () -> new Worker<T, T>() {
            T min = null;

            @Override
            public void accept(T t) {
                if (min == null || comparator.compare(min, t) > 0) {
                    min = t;
                }
            }

            @Override
            public T result() {
                return min;
            }
        };
    }

    static <T, V extends Comparable<V>> Reducer<T, Pair<T, V>> minBy(Function<T, V> function) {
        return () -> new Worker<T, Pair<T, V>>() {
            T min = null;
            V val = null;

            @Override
            public void accept(T t) {
                V v = function.apply(t);
                if (val == null || val.compareTo(v) > 0) {
                    min = t;
                    val = v;
                }
            }

            @Override
            public Pair<T, V> result() {
                return new Pair<>(min, val);
            }
        };
    }

    static <T> Reducer<T, IntPair<T>> minByInt(ToIntFunction<T> function) {
        return () -> new Worker<T, IntPair<T>>() {
            T min = null;
            int val = 0;

            @Override
            public void accept(T t) {
                int v = function.applyAsInt(t);
                if (min == null || val > v) {
                    min = t;
                    val = v;
                }
            }

            @Override
            public IntPair<T> result() {
                return new IntPair<>(val, min);
            }
        };
    }

    static <T> Reducer<T, DoublePair<T>> minByDouble(ToDoubleFunction<T> function) {
        return () -> new Worker<T, DoublePair<T>>() {
            T min = null;
            double val = 0;

            @Override
            public void accept(T t) {
                double v = function.applyAsDouble(t);
                if (min == null || val > v) {
                    min = t;
                    val = v;
                }
            }

            @Override
            public DoublePair<T> result() {
                return new DoublePair<>(val, min);
            }
        };
    }

    static <T> Reducer<T, LongPair<T>> minByLong(ToLongFunction<T> function) {
        return () -> new Worker<T, LongPair<T>>() {
            T min = null;
            long val = 0;

            @Override
            public void accept(T t) {
                long v = function.applyAsLong(t);
                if (min == null || val > v) {
                    min = t;
                    val = v;
                }
            }

            @Override
            public LongPair<T> result() {
                return new LongPair<>(val, min);
            }
        };
    }

    static <T, V, E> Reducer<T, E> of(Collector<T, V, E> collector) {
        Supplier<V> supplier = collector.supplier();
        BiConsumer<V, T> accumulator = collector.accumulator();
        Function<V, E> finisher = collector.finisher();
        return () -> new Worker<T, E>() {
            final V v = supplier.get();

            @Override
            public void accept(T t) {
                accumulator.accept(v, t);
            }

            @Override
            public E result() {
                return finisher.apply(v);
            }
        };
    }

    static <T, V> Reducer<T, V> of(Supplier<V> supplier, BiConsumer<V, T> accumulator) {
        return () -> new Worker<T, V>() {
            final V v = supplier.get();

            @Override
            public void accept(T t) {
                accumulator.accept(v, t);
            }

            @Override
            public V result() {
                return v;
            }
        };
    }

    static <T, V> Reducer<T, V> of(Supplier<V> supplier, BiConsumer<V, T> accumulator, Consumer<V> finisher) {
        return () -> new Worker<T, V>() {
            final V v = supplier.get();

            @Override
            public void accept(T t) {
                accumulator.accept(v, t);
            }

            @Override
            public V result() {
                finisher.accept(v);
                return v;
            }
        };
    }

    static <T> Reducer<T, Pair<SeqList<T>, SeqList<T>>> partition(Predicate<T> predicate) {
        return partition(predicate, toList());
    }

    static <T, V> Reducer<T, Pair<V, V>> partition(Predicate<T> predicate, Reducer<T, V> reducer) {
        return () -> new Worker<T, Pair<V, V>>() {
            final Worker<T, V> first = reducer.get();
            final Worker<T, V> second = reducer.get();

            @Override
            public void accept(T t) {
                if (predicate.test(t)) {
                    first.accept(t);
                } else {
                    second.accept(t);
                }
            }

            @Override
            public Pair<V, V> result() {
                return new Pair<>(first.result(), second.result());
            }
        };
    }

    static <T> Reducer<T, SeqList<T>> reverse() {
        return Reducer.<T>toList().then(Collections::reverse);
    }

    static <T> Reducer<T, SeqList<T>> sort() {
        return sort((Comparator<T>)null);
    }

    static <T> Reducer<T, SeqList<T>> sort(Comparator<T> comparator) {
        return Reducer.<T>toList().then(ts -> ts.sort(comparator));
    }

    static <T, V extends Comparable<V>> Reducer<T, SeqList<T>> sort(Function<T, V> function) {
        return sort(Comparator.comparing(function));
    }

    static <T> Reducer<T, SeqList<T>> sortDesc() {
        return sort(Collections.reverseOrder());
    }

    static <T> Reducer<T, SeqList<T>> sortDesc(Comparator<T> comparator) {
        return sort(comparator.reversed());
    }

    static <T, V extends Comparable<V>> Reducer<T, SeqList<T>> sortDesc(Function<T, V> function) {
        return sort(Comparator.comparing(function).reversed());
    }

    static Reducer<Double, Double> sum() {
        return () -> new Worker<Double, Double>() {
            double s = 0;

            @Override
            public void accept(Double t) {
                s += t;
            }

            @Override
            public Double result() {
                return s;
            }
        };
    }

    static <T> Reducer<T, Double> sum(ToDoubleFunction<T> function) {
        return () -> new Worker<T, Double>() {
            double s = 0;

            @Override
            public void accept(T t) {
                s += function.applyAsDouble(t);
            }

            @Override
            public Double result() {
                return s;
            }
        };
    }

    static Reducer<Integer, Integer> sumInt() {
        return () -> new Worker<Integer, Integer>() {
            int s = 0;

            @Override
            public void accept(Integer t) {
                s += t;
            }

            @Override
            public Integer result() {
                return s;
            }
        };
    }

    static <T> Reducer<T, Integer> sumInt(ToIntFunction<T> function) {
        return () -> new Worker<T, Integer>() {
            int s = 0;

            @Override
            public void accept(T t) {
                s += function.applyAsInt(t);
            }

            @Override
            public Integer result() {
                return s;
            }
        };
    }

    static Reducer<Long, Long> sumLong() {
        return () -> new Worker<Long, Long>() {
            long s = 0;

            @Override
            public void accept(Long t) {
                s += t;
            }

            @Override
            public Long result() {
                return s;
            }
        };
    }

    static <T> Reducer<T, Long> sumLong(ToLongFunction<T> function) {
        return () -> new Worker<T, Long>() {
            long s = 0;

            @Override
            public void accept(T t) {
                s += function.applyAsLong(t);
            }

            @Override
            public Long result() {
                return s;
            }
        };
    }

    static <T> Reducer<T, BatchedSeq<T>> toBatched() {
        return of(BatchedSeq::new, BatchedSeq::add);
    }

    static <T> Reducer<T, ConcurrentSeq<T>> toConcurrent() {
        return of(ConcurrentSeq::new, ConcurrentSeq::add);
    }

    static <T> Reducer<T, LinkedSeq<T>> toLinked() {
        return of(LinkedSeq::new, LinkedSeq::add);
    }

    static <T> Reducer<T, SeqList<T>> toList() {
        return of(SeqList::new, SeqList::add);
    }

    static <T> Reducer<T, SeqList<T>> toList(int initialCapacity) {
        return of(() -> new SeqList<>(initialCapacity), SeqList::add);
    }

    static <T, K, V> Reducer<T, SeqMap<K, V>> toMap(Function<T, K> toKey, Function<T, V> toValue) {
        return of(SeqMap::new, (m, t) -> m.put(toKey.apply(t), toValue.apply(t)));
    }

    static <T, K, V, M extends Map<K, V>> Reducer<T, M> toMap(Supplier<M> mapSupplier, Function<T, K> toKey, Function<T, V> toValue) {
        return of(mapSupplier, (m, t) -> m.put(toKey.apply(t), toValue.apply(t)));
    }

    static <T, K> Reducer<T, SeqMap<K, T>> toMapBy(Function<T, K> toKey) {
        return toMapBy(SeqMap::new, toKey);
    }

    static <T, K, M extends Map<K, T>> Reducer<T, M> toMapBy(Supplier<M> mapSupplier, Function<T, K> toKey) {
        return of(mapSupplier, (m, t) -> m.put(toKey.apply(t), t));
    }

    static <T, V> Reducer<T, SeqMap<T, V>> toMapWith(Function<T, V> toValue) {
        return toMapWith(SeqMap::new, toValue);
    }

    static <T, V, M extends Map<T, V>> Reducer<T, M> toMapWith(Supplier<M> mapSupplier, Function<T, V> toValue) {
        return of(mapSupplier, (m, t) -> m.put(t, toValue.apply(t)));
    }

    static <T> Reducer<T, SeqSet<T>> toSet() {
        return of(SeqSet::new, Set::add);
    }

    static <T> Reducer<T, SeqSet<T>> toSet(int initialCapacity) {
        return of(() -> new SeqSet<>(initialCapacity), Set::add);
    }

    default Reducer<T, V> then(Consumer<V> action) {
        Reducer<T, V> reducer = this;
        return () -> new Worker<T, V>() {
            final Worker<T, V> worker = reducer.get();

            @Override
            public void accept(T t) {
                worker.accept(t);
            }

            @Override
            public V result() {
                V res = worker.result();
                action.accept(res);
                return res;
            }
        };
    }

    interface Worker<T, V> {
        void accept(T t);
        V result();
    }
}

package com.github.wolray.seq;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;

/**
 * @author wolray
 */
public interface Reducer<T, V> extends Predicate<T> {
    V result();
    boolean done();

    static <T> Reducer<T, Boolean> any(Predicate<T> predicate) {
        return new SignalReducer<T, Boolean>() {
            @Override
            protected boolean accept(T t) {
                return predicate.test(t);
            }

            @Override
            public Boolean result() {
                return done;
            }
        };
    }

    static <T> Reducer<T, Double> average(BiConsumer<AverageFolder, T> consumer) {
        return collect(new AverageFolder(), consumer, AverageFolder::result);
    }

    static <T, C extends Collection<T>> Reducer<T, C> collect(C des) {
        return of(des, Collection::add);
    }

    static <T, V, E> Reducer<T, E> collect(Collector<T, V, E> collector) {
        return collect(collector.supplier().get(), collector.accumulator(), collector.finisher());
    }

    static <T, V, E> Reducer<T, E> collect(V des, BiConsumer<V, T> accumulator, Function<V, E> finisher) {
        return new SimpleReducer<T, E>() {
            @Override
            protected void accept(T t) {
                accumulator.accept(des, t);
            }

            @Override
            public E result() {
                return finisher.apply(des);
            }
        };
    }

    static <T> Reducer<T, Integer> count() {
        return new SimpleReducer<T, Integer>() {
            int cnt = 0;

            @Override
            protected void accept(T t) {
                cnt++;
            }

            @Override
            public Integer result() {
                return cnt;
            }
        };
    }

    static <T> Reducer<T, Integer> count(Predicate<T> predicate) {
        return new SimpleReducer<T, Integer>() {
            int cnt = 0;

            @Override
            protected void accept(T t) {
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

    static <T> Reducer<T, Optional<T>> find(Predicate<T> predicate) {
        return new SignalReducer<T, Optional<T>>() {
            T value;

            @Override
            protected boolean accept(T t) {
                if (predicate.test(t)) {
                    value = t;
                    return true;
                }
                return false;
            }

            @Override
            public Optional<T> result() {
                return done ? Optional.ofNullable(value) : Optional.empty();
            }
        };
    }

    static <T> Reducer<T, T> first() {
        return new SignalReducer<T, T>() {
            T value;

            @Override
            protected boolean accept(T t) {
                value = t;
                return true;
            }

            @Override
            public T result() {
                return value;
            }
        };
    }

    static <T> Reducer<T, T> first(Predicate<T> predicate) {
        return new SignalReducer<T, T>() {
            T value;

            @Override
            protected boolean accept(T t) {
                if (predicate.test(t)) {
                    value = t;
                    return true;
                }
                return false;
            }

            @Override
            public T result() {
                return value;
            }
        };
    }

    static <T> Reducer<T, T> fold(BinaryOperator<T> operator) {
        return new SimpleReducer<T, T>() {
            T cur = null;

            @Override
            protected void accept(T t) {
                cur = cur == null ? t : operator.apply(cur, t);
            }

            @Override
            public T result() {
                return cur;
            }
        };
    }

    static <T> Reducer<T, T> fold(T seed, BinaryOperator<T> operator) {
        return new SimpleReducer<T, T>() {
            T cur = seed;

            @Override
            protected void accept(T t) {
                cur = operator.apply(cur, t);
            }

            @Override
            public T result() {
                return cur;
            }
        };
    }

    static <T, K, V> Reducer<T, SeqMap<K, V>> groupBy(Function<T, K> toKey, Supplier<Reducer<T, V>> factory) {
        return new SimpleReducer<T, SeqMap<K, V>>() {
            final SeqMap<K, Reducer<T, V>> map = new SeqMap<>();

            @Override
            protected void accept(T t) {
                map.getOrCompute(toKey.apply(t), factory).test(t);
            }

            @Override
            public SeqMap<K, V> result() {
                return map.mapValues(Reducer::result);
            }
        };
    }

    static Reducer<String, String> join(String sep) {
        return new SimpleReducer<String, String>() {
            final StringJoiner joiner = new StringJoiner(sep);

            @Override
            protected void accept(String t) {
                joiner.add(t);
            }

            @Override
            public String result() {
                return joiner.toString();
            }
        };
    }

    static <T> Reducer<T, String> join(String sep, Function<T, String> function) {
        return new SimpleReducer<T, String>() {
            final StringJoiner joiner = new StringJoiner(sep);

            @Override
            protected void accept(T t) {
                joiner.add(function.apply(t));
            }

            @Override
            public String result() {
                return joiner.toString();
            }
        };
    }

    static <T> Reducer<T, T> last() {
        return new SimpleReducer<T, T>() {
            T value;

            @Override
            protected void accept(T t) {
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

    static <T, E, V> Reducer<T, V> mapping(Function<T, E> before, Reducer<E, V> reducer) {
        return new SignalReducer<T, V>() {
            @Override
            protected boolean accept(T t) {
                return reducer.test(before.apply(t));
            }

            @Override
            public V result() {
                return reducer.result();
            }
        };
    }

    static <T, V, E> Reducer<T, E> mapping(Reducer<T, V> reducer, Function<V, E> after) {
        return new SignalReducer<T, E>() {
            @Override
            protected boolean accept(T t) {
                return reducer.test(t);
            }

            @Override
            public E result() {
                return after.apply(reducer.result());
            }
        };
    }

    static <T> Reducer<T, T> max(Comparator<T> comparator) {
        return new SimpleReducer<T, T>() {
            T max = null;

            @Override
            protected void accept(T t) {
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
        return new SimpleReducer<T, Pair<T, V>>() {
            T max = null;
            V val = null;

            @Override
            protected void accept(T t) {
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
        return new SimpleReducer<T, IntPair<T>>() {
            T max = null;
            int val = 0;

            @Override
            protected void accept(T t) {
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
        return new SimpleReducer<T, DoublePair<T>>() {
            T max = null;
            double val = 0;

            @Override
            protected void accept(T t) {
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
        return new SimpleReducer<T, LongPair<T>>() {
            T max = null;
            long val = 0;

            @Override
            protected void accept(T t) {
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
        return new SimpleReducer<T, T>() {
            T min = null;

            @Override
            protected void accept(T t) {
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
        return new SimpleReducer<T, Pair<T, V>>() {
            T min = null;
            V val = null;

            @Override
            protected void accept(T t) {
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
        return new SimpleReducer<T, IntPair<T>>() {
            T min = null;
            int val = 0;

            @Override
            protected void accept(T t) {
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
        return new SimpleReducer<T, DoublePair<T>>() {
            T min = null;
            double val = 0;

            @Override
            protected void accept(T t) {
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
        return new SimpleReducer<T, LongPair<T>>() {
            T min = null;
            long val = 0;

            @Override
            protected void accept(T t) {
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

    static <T, E> Reducer<T, SeqList<E>> of(Downstream<T, E> downstream) {
        return of(downstream, toList());
    }

    static <T, E, V> Reducer<T, V> of(Downstream<T, E> downstream, Reducer<E, V> reducer) {
        Predicate<T> predicate = downstream.apply(reducer);
        return new SignalReducer<T, V>() {
            @Override
            protected boolean accept(T t) {
                return predicate.test(t);
            }

            @Override
            public V result() {
                return reducer.result();
            }
        };
    }

    static <T, V> Reducer<T, V> of(V des, BiConsumer<V, T> accumulator) {
        return new SimpleReducer<T, V>() {
            @Override
            protected void accept(T t) {
                accumulator.accept(des, t);
            }

            @Override
            public V result() {
                return des;
            }
        };
    }

    static <T, V> Reducer<T, V> of(V des, BiConsumer<V, T> accumulator, Consumer<V> finisher) {
        return new SimpleReducer<T, V>() {
            @Override
            protected void accept(T t) {
                accumulator.accept(des, t);
            }

            @Override
            public V result() {
                finisher.accept(des);
                return des;
            }
        };
    }

    static <T, E> Reducer<T, SeqList<E>> ofStaged(Downstream.Staged<T, E> downstream) {
        return ofStaged(downstream, toList());
    }

    static <T, E, V> Reducer<T, V> ofStaged(Downstream.Staged<T, E> downstream, Reducer<E, V> reducer) {
        Downstream.StagedPredicate<T> predicate = downstream.apply(reducer);
        return new SignalReducer<T, V>() {
            @Override
            protected boolean accept(T t) {
                return predicate.test(t);
            }

            @Override
            public V result() {
                if (!done) {
                    done = predicate.after();
                }
                return reducer.result();
            }
        };
    }

    static <T> Reducer<T, Pair<SeqList<T>, SeqList<T>>> partition(Predicate<T> predicate) {
        return partition(predicate, Reducer::toList);
    }

    static <T, V> Reducer<T, Pair<V, V>> partition(Predicate<T> predicate, Supplier<Reducer<T, V>> factory) {
        return partition(predicate, factory.get(), factory.get());
    }

    static <T, K, V> Reducer<T, Pair<K, V>> partition(Predicate<T> predicate, Reducer<T, K> r1, Reducer<T, V> r2) {
        return new SignalReducer<T, Pair<K, V>>() {
            @Override
            protected boolean accept(T t) {
                if (predicate.test(t)) {
                    r1.test(t);
                } else {
                    r2.test(t);
                }
                return r1.done() && r2.done();
            }

            @Override
            public Pair<K, V> result() {
                return new Pair<>(r1.result(), r2.result());
            }
        };
    }

    static <T> Reducer<T, SeqList<T>> reverse() {
        return of(new SeqList<>(), SeqList::add, Collections::reverse);
    }

    static <T> Reducer<T, SeqList<T>> sort() {
        return sort((Comparator<T>)null);
    }

    static <T> Reducer<T, SeqList<T>> sort(Comparator<T> comparator) {
        return of(new SeqList<>(), SeqList::add, ts -> ts.sort(comparator));
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
        return new SimpleReducer<Double, Double>() {
            double s = 0;

            @Override
            protected void accept(Double t) {
                s += t;
            }

            @Override
            public Double result() {
                return s;
            }
        };
    }

    static <T> Reducer<T, Double> sum(ToDoubleFunction<T> function) {
        return new SimpleReducer<T, Double>() {
            double s = 0;

            @Override
            protected void accept(T t) {
                s += function.applyAsDouble(t);
            }

            @Override
            public Double result() {
                return s;
            }
        };
    }

    static Reducer<Integer, Integer> sumInt() {
        return new SimpleReducer<Integer, Integer>() {
            int s = 0;

            @Override
            protected void accept(Integer t) {
                s += t;
            }

            @Override
            public Integer result() {
                return s;
            }
        };
    }

    static <T> Reducer<T, Integer> sumInt(ToIntFunction<T> function) {
        return new SimpleReducer<T, Integer>() {
            int s = 0;

            @Override
            protected void accept(T t) {
                s += function.applyAsInt(t);
            }

            @Override
            public Integer result() {
                return s;
            }
        };
    }

    static Reducer<Long, Long> sumLong() {
        return new SimpleReducer<Long, Long>() {
            long s = 0;

            @Override
            protected void accept(Long t) {
                s += t;
            }

            @Override
            public Long result() {
                return s;
            }
        };
    }

    static <T> Reducer<T, Long> sumLong(ToLongFunction<T> function) {
        return new SimpleReducer<T, Long>() {
            long s = 0;

            @Override
            protected void accept(T t) {
                s += function.applyAsLong(t);
            }

            @Override
            public Long result() {
                return s;
            }
        };
    }

    static <T, V> Reducer<T, V> then(Reducer<T, V> reducer, Consumer<V> action) {
        return new SignalReducer<T, V>() {
            @Override
            protected boolean accept(T t) {
                return reducer.test(t);
            }

            @Override
            public V result() {
                V res = reducer.result();
                action.accept(res);
                return res;
            }
        };
    }

    static <T> Reducer<T, BatchedSeq<T>> toBatched() {
        return of(new BatchedSeq<>(), BatchedSeq::add);
    }

    static <T> Reducer<T, ConcurrentSeq<T>> toConcurrent() {
        return collect(new ConcurrentSeq<>());
    }

    static <T> Reducer<T, LinkedSeq<T>> toLinked() {
        return collect(new LinkedSeq<>());
    }

    static <T> Reducer<T, SeqList<T>> toList() {
        return collect(new SeqList<>());
    }

    static <T> Reducer<T, SeqList<T>> toList(int initialCapacity) {
        return collect(new SeqList<>(initialCapacity));
    }

    static <T, K, V> Reducer<T, SeqMap<K, V>> toMap(BiConsumer<SeqMap<K, V>, T> consumer) {
        return of(new SeqMap<>(), consumer);
    }

    static <T> Reducer<T, SeqSet<T>> toSet() {
        return collect(new SeqSet<>());
    }

    static <T> Reducer<T, SeqSet<T>> toSet(int initialCapacity) {
        return collect(new SeqSet<>(initialCapacity));
    }

    abstract class SignalReducer<T, V> implements Reducer<T, V> {
        protected boolean done;

        protected abstract boolean accept(T t);

        @Override
        public final boolean test(T t) {
            return done || (done = accept(t));
        }

        public final boolean done() {
            return done;
        }
    }

    abstract class SimpleReducer<T, V> implements Reducer<T, V> {
        protected abstract void accept(T t);

        @Override
        public final boolean test(T t) {
            accept(t);
            return false;
        }

        @Override
        public final boolean done() {
            return false;
        }
    }
}

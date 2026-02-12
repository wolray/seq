package com.github.wolray.seq;

import java.util.*;
import java.util.function.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author wolray
 */
public interface Seq<T> {
    boolean until(Predicate<T> stop);

    static <T> ItrSeq<T> empty() {
        return Collections::emptyIterator;
    }

    @SafeVarargs
    static <T> ItrSeq<T> flatIterable(Iterable<T>... iterables) {
        return () -> ItrSeq.flatIterable(Arrays.asList(iterables));
    }

    static <T> ItrSeq<T> flatOptional(Iterable<Optional<T>> iterable) {
        return ItrSeq.copyIf(iterable, (p, t) -> t.filter(p::set).isPresent());
    }

    static <T> ItrSeq<T> gen(Supplier<T> supplier) {
        return () -> new Puller<T>() {
            @Override
            public boolean hasNext() {
                return set(supplier.get());
            }
        };
    }

    static <T> ItrSeq<T> gen(T seed, UnaryOperator<T> operator) {
        return () -> new Puller<T>() {
            T t = seed;

            @Override
            public boolean hasNext() {
                if (index == 0) {
                    return setAndIncrease(t);
                } else {
                    return set(t = operator.apply(t));
                }
            }
        };
    }

    static <T> ItrSeq<T> gen(T seed1, T seed2, BinaryOperator<T> operator) {
        return () -> new Puller<T>() {
            T t1 = seed1, t2 = seed2;

            @Override
            public boolean hasNext() {
                if (index == 0) {
                    return setAndIncrease(t1);
                } else if (index == 1) {
                    return setAndIncrease(t2);
                } else {
                    return set(t2 = operator.apply(t1, t1 = t2));
                }
            }
        };
    }

    static <T> ItrSeq<T> of(Iterable<T> iterable) {
        if (iterable instanceof ItrSeq) {
            return (ItrSeq<T>)iterable;
        }
        if (iterable instanceof Collection) {
            Collection<T> collection = (Collection<T>)iterable;
            return new SizedSeq<T>() {
                @Override
                public Iterator<T> iterator() {
                    return iterable.iterator();
                }

                @Override
                public int size() {
                    return collection.size();
                }
            };
        }
        return iterable::iterator;
    }

    @SafeVarargs
    static <T> ItrSeq<T> of(T... ts) {
        return of(Arrays.asList(ts));
    }

    static ItrSeq<Integer> range(int n) {
        return () -> new Puller<Integer>() {
            @Override
            public boolean hasNext() {
                return index < n && setAndIncrease(index);
            }
        };
    }

    static ItrSeq<Integer> range(int start, int stop) {
        return () -> new Puller<Integer>() {
            {
                index = start;
            }

            @Override
            public boolean hasNext() {
                return index < stop && setAndIncrease(index);
            }
        };
    }

    static <T> ItrSeq<T> repeat(int n, T t) {
        return () -> new Puller<T>() {
            @Override
            public boolean hasNext() {
                return index < n && setAndIncrease(t);
            }
        };
    }

    static <T> ItrSeq<T> untilNull(Supplier<T> supplier) {
        return () -> new Puller<T>() {
            @Override
            public boolean hasNext() {
                T t = supplier.get();
                return t != null && set(t);
            }
        };
    }

    @SafeVarargs
    static <T> Seq<T> direct(T... ts) {
        return p -> {
            for (T t : ts) {
                if (p.test(t)) {
                    return true;
                }
            }
            return false;
        };
    }

    static <T> Seq<T> flat(Seq<Optional<T>> seq) {
        return p -> seq.until(o -> o.filter(p).isPresent());
    }

    @SafeVarargs
    static <T> Seq<T> flat(Seq<T>... seq) {
        return p -> {
            for (Seq<T> s : seq) {
                if (s.until(p)) {
                    return true;
                }
            }
            return false;
        };
    }

    static Seq<Matcher> match(String s, Pattern pattern) {
        return p -> {
            Matcher matcher = pattern.matcher(s);
            while (matcher.find()) {
                if (p.test(matcher)) {
                    return true;
                }
            }
            return false;
        };
    }

    static Seq<Object> ofJson(Object node) {
        return Seq.ofTree(node, n -> p -> {
            if (n instanceof Iterable) {
                for (Object o : ((Iterable<?>)n)) {
                    if (p.test(o)) {
                        return true;
                    }
                }
            } else if (n instanceof Map) {
                for (Object value : ((Map<?, ?>)n).values()) {
                    if (p.test(value)) {
                        return true;
                    }
                }
            }
            return false;
        });
    }

    static <N> Seq<N> ofTree(N node, Function<N, Seq<N>> sub) {
        return SeqExpand.of(sub).toSeq(node);
    }

    static <N> Seq<N> ofTree(int maxDepth, N node, Function<N, Seq<N>> sub) {
        return SeqExpand.of(sub).toSeq(node, maxDepth);
    }

    static <T> Seq<T> unit(T t) {
        return p -> p.test(t);
    }

    default void consume(Consumer<T> consumer) {
        until(t -> {
            consumer.accept(t);
            return false;
        });
    }

    default void consumeIndexed(IntObjConsumer<T> consumer) {
        consume(new Consumer<T>() {
            int index = 0;

            @Override
            public void accept(T t) {
                consumer.accept(index++, t);
            }
        });
    }

    default void printAll(String sep) {
        if ("\n".equals(sep)) {
            println();
        } else {
            System.out.println(join(sep, Objects::toString));
        }
    }

    default void println() {
        consume(System.out::println);
    }

    default BatchedSeq<T> toBatched() {
        return reduce(new BatchedSeq<>(), BatchedSeq::add);
    }

    default <C extends Collection<T>> C collectBy(IntFunction<C> constructor) {
        return reduce(constructor.apply(sizeOrDefault()), Collection::add);
    }

    default ConcurrentSeq<T> toConcurrent() {
        return reduce(new ConcurrentSeq<>(), ConcurrentSeq::add);
    }

    default <E> E reduce(Reducer<T, E> reducer) {
        Reducer.Worker<T, E> worker = reducer.get();
        consume(worker::accept);
        return worker.result();
    }

    default <E> E reduce(E des, BiConsumer<E, T> accumulator) {
        consume(t -> accumulator.accept(des, t));
        return des;
    }

    default <E, V> E reduce(Reducer<T, V> reducer, Function<V, E> function) {
        return function.apply(reduce(reducer));
    }

    default ItrSeq<T> asIterable() {
        return toBatched();
    }

    default <E> Lazy<E> toLazy(Reducer<T, E> reducer) {
        return Lazy.of(() -> reduce(reducer));
    }

    default LinkedSeq<T> toLinked() {
        return reduce(new LinkedSeq<>(), LinkedSeq::add);
    }

    default Optional<T> find(Predicate<T> predicate) {
        return reduce(Reducer.find(predicate));
    }

    default Optional<T> findDuplicate() {
        Set<T> set = new HashSet<>(sizeOrDefault());
        return find(t -> !set.add(t));
    }

    default Optional<T> findFirst() {
        return find(t -> true);
    }

    default Optional<T> findNot(Predicate<T> predicate) {
        return find(predicate.negate());
    }

    default Optional<T> last(Predicate<T> predicate) {
        return filter(predicate).lastMaybe();
    }

    default Optional<T> lastMaybe() {
        Mutable<T> m = new Mutable<>(null);
        consume(m::set);
        return m.toOptional();
    }

    default Optional<T> lastNot(Predicate<T> predicate) {
        return last(predicate.negate());
    }

    default <V extends Comparable<V>> Pair<T, V> maxBy(Function<T, V> function) {
        return reduce(Reducer.maxBy(function));
    }

    default <V extends Comparable<V>> Pair<T, V> minBy(Function<T, V> function) {
        return reduce(Reducer.minBy(function));
    }

    default Seq<T> append(T t) {
        return p -> {
            until(p);
            return p.test(t);
        };
    }

    @SuppressWarnings("unchecked")
    default Seq<T> append(T... t) {
        return p -> {
            if (until(p)) {
                return true;
            }
            for (T x : t) {
                if (p.test(x)) {
                    return true;
                }
            }
            return false;
        };
    }

    default Seq<T> appendAll(Iterable<T> iterable) {
        return p -> {
            if (until(p)) {
                return true;
            }
            for (T t : iterable) {
                if (p.test(t)) {
                    return true;
                }
            }
            return false;
        };
    }

    default Seq<T> appendWith(Seq<T> seq) {
        return p -> until(p) || seq.until(p);
    }

    default Seq<SeqList<T>> chunked(int size) {
        return chunked(size, Reducer.toList(size));
    }

    default <V> Seq<V> chunked(int size, Reducer<T, V> reducer) {
        if (size <= 0) {
            throw new IllegalArgumentException("non-positive size");
        }
        return p -> {
            Mutable<Reducer.Worker<T, V>> m = new Mutable<>(null);
            m.it = reducer.get();
            boolean flag = until(new Predicate<T>() {
                int idx = 0;

                @Override
                public boolean test(T t) {
                    if (idx == size) {
                        if (p.test(m.it.result())) {
                            m.it = null;
                            return true;
                        }
                        m.it = reducer.get();
                        idx = 0;
                    }
                    m.it.accept(t);
                    idx++;
                    return false;
                }
            });
            if (flag) {
                return true;
            }
            if (m.it != null) {
                return p.test(m.it.result());
            }
            return false;
        };
    }

    default Seq<T> circle() {
        return p -> {
            while (true) {
                if (until(p)) {
                    return true;
                }
            }
        };
    }

    default Seq<T> distinct() {
        return p -> {
            HashSet<T> set = new HashSet<>();
            return until(t -> set.add(t) && p.test(t));
        };
    }

    default <E> Seq<T> distinctBy(Function<T, E> function) {
        return p -> {
            HashSet<E> set = new HashSet<>();
            return until(t -> set.add(function.apply(t)) && p.test(t));
        };
    }

    default Seq<T> drop(int n) {
        return n <= 0 ? this : p -> untilIndexed((i, t) -> i >= n && p.test(t));
    }

    default Seq<T> dropWhile(Predicate<T> predicate) {
        return p -> until(new Predicate<T>() {
            boolean flag = true;

            @Override
            public boolean test(T t) {
                if (flag) {
                    if (predicate.test(t)) {
                        return false;
                    }
                    flag = false;
                }
                return p.test(t);
            }
        });
    }

    default Seq<T> duplicateAll(int times) {
        return p -> {
            for (int i = 0; i < times; i++) {
                if (until(p)) {
                    return true;
                }
            }
            return false;
        };
    }

    default Seq<T> duplicateEach(int times) {
        return p -> until(t -> {
            for (int i = 0; i < times; i++) {
                if (p.test(t)) {
                    return true;
                }
            }
            return false;
        });
    }

    default Seq<T> duplicateIf(int times, Predicate<T> predicate) {
        return p -> until(t -> {
            if (predicate.test(t)) {
                for (int i = 0; i < times; i++) {
                    if (p.test(t)) {
                        return true;
                    }
                }
            } else {
                return p.test(t);
            }
            return false;
        });
    }

    default Seq<T> filter(Predicate<T> predicate) {
        return p -> until(t -> predicate.test(t) && p.test(t));
    }

    default Seq<T> filterIn(Collection<T> collection) {
        return filter(collection::contains);
    }

    default Seq<T> filterIn(Map<T, ?> map) {
        return filter(map::containsKey);
    }

    default Seq<T> filterIndexed(IntObjPredicate<T> predicate) {
        return p -> untilIndexed((i, t) -> predicate.test(i, t) && p.test(t));
    }

    default <E extends T> Seq<E> filterInstance(Class<E> cls) {
        return p -> until(t -> cls.isInstance(t) && p.test(cls.cast(t)));
    }

    default Seq<T> filterNot(Predicate<T> predicate) {
        return filter(predicate.negate());
    }

    default Seq<T> filterNotIn(Collection<T> collection) {
        return filterNot(collection::contains);
    }

    default Seq<T> filterNotIn(Map<T, ?> map) {
        return filterNot(map::containsKey);
    }

    default Seq<T> filterNotNull() {
        return filter(Objects::nonNull);
    }

    default <E> Seq<E> flatIterable(Function<T, Iterable<E>> function) {
        return p -> until(t -> {
            for (E e : function.apply(t)) {
                if (p.test(e)) {
                    return true;
                }
            }
            return false;
        });
    }

    default <E> Seq<E> flatMap(Function<T, Seq<E>> function) {
        return p -> until(t -> function.apply(t).until(p));
    }

    default <E> Seq<E> flatOptional(Function<T, Optional<E>> function) {
        return p -> until(t -> function.apply(t).filter(p).isPresent());
    }

    default <E> Seq<E> map(Function<T, E> function) {
        return p -> until(t -> p.test(function.apply(t)));
    }

    default <E> Seq<E> mapIf(BiPredicate<Predicate<E>, T> predicate) {
        return p -> until(t -> predicate.test(p, t));
    }

    default <E> Seq<E> mapIndexed(IntObjFunction<T, E> function) {
        return p -> untilIndexed((i, t) -> p.test(function.apply(i, t)));
    }

    default Seq<T> onEach(Consumer<T> consumer) {
        return p -> until(t -> {
            consumer.accept(t);
            return p.test(t);
        });
    }

    default Seq<T> onEachIndexed(IntObjConsumer<T> consumer) {
        return p -> untilIndexed((i, t) -> {
            consumer.accept(i, t);
            return p.test(t);
        });
    }

    default Seq<T> replace(int n, UnaryOperator<T> operator) {
        return mapIndexed((i, t) -> i < n ? operator.apply(t) : t);
    }

    default <E> Seq<E> runningFold(E init, BiFunction<E, T, E> function) {
        return p -> until(new Predicate<T>() {
            E cur = init;

            @Override
            public boolean test(T t) {
                return p.test(cur = function.apply(cur, t));
            }
        });
    }

    default <E extends Comparable<E>> Seq<T> sortCached(Function<T, E> function) {
        return map(t -> new Pair<>(t, function.apply(t))).sortBy(p -> p.second).map(p -> p.first);
    }

    default <E extends Comparable<E>> Seq<T> sortCachedDesc(Function<T, E> function) {
        return map(t -> new Pair<>(t, function.apply(t))).sortByDesc(p -> p.second).map(p -> p.first);
    }

    default Seq<T> take(int n) {
        return p -> until(new Predicate<T>() {
            int i = 1;

            @Override
            public boolean test(T t) {
                return i++ > n || p.test(t);
            }
        });
    }

    default Seq<T> takeWhile(BiPredicate<T, T> testPrevCurr) {
        return p -> until(new Predicate<T>() {
            T prev = null;
            boolean first = true;

            @Override
            public boolean test(T t) {
                if (first) {
                    first = false;
                    prev = t;
                    return p.test(t);
                } else {
                    if (testPrevCurr.test(prev, t)) {
                        prev = t;
                        return p.test(t);
                    }
                    return true;
                }
            }
        });
    }

    default Seq<T> takeWhile(Predicate<T> predicate) {
        return p -> until(t -> !predicate.test(t) || p.test(t));
    }

    default Seq<T> takeWhileEquals() {
        return takeWhile(Objects::equals);
    }

    default Seq<T> timeLimit(long millis) {
        return millis <= 0 ? this : p -> {
            long end = System.currentTimeMillis() + millis;
            return until(t -> System.currentTimeMillis() > end || p.test(t));
        };
    }

    default Seq<SeqList<T>> windowed(int size, int step, boolean allowPartial) {
        return windowed(size, step, allowPartial, Reducer.toList());
    }

    default <V> Seq<V> windowed(int size, int step, boolean allowPartial, Reducer<T, V> reducer) {
        if (size <= 0 || step <= 0) {
            throw new IllegalArgumentException("non-positive size or step");
        }
        return p -> {
            Queue<IntPair<Reducer.Worker<T, V>>> queue = new LinkedList<>();
            boolean flag = until(new Predicate<T>() {
                int i = 0;

                @Override
                public boolean test(T t) {
                    if (i == 0) {
                        i = step;
                        queue.offer(new IntPair<>(0, reducer.get()));
                    }
                    queue.forEach(sub -> {
                        sub.it.accept(t);
                        sub.intVal++;
                    });
                    IntPair<Reducer.Worker<T, V>> first = queue.peek();
                    if (first != null && first.intVal == size) {
                        queue.poll();
                        if (p.test(first.it.result())) {
                            return true;
                        }
                    }
                    i -= 1;
                    return false;
                }
            });
            if (flag) {
                return true;
            }
            if (allowPartial) {
                queue.forEach(pair -> p.test(pair.it.result()));
            }
            queue.clear();
            return false;
        };
    }

    default Seq<SeqList<T>> windowedByTime(long timeMillis) {
        return windowedByTime(timeMillis, Reducer.toList());
    }

    default <V> Seq<V> windowedByTime(long timeMillis, Reducer<T, V> reducer) {
        if (timeMillis <= 0) {
            throw new IllegalArgumentException("non-positive time");
        }
        return p -> until(new Predicate<T>() {
            long last = System.currentTimeMillis();
            Reducer.Worker<T, V> worker = reducer.get();

            @Override
            public boolean test(T t) {
                long now = System.currentTimeMillis();
                if (now - last > timeMillis) {
                    last = now;
                    if (p.test(worker.result())) {
                        return true;
                    }
                    worker = reducer.get();
                }
                worker.accept(t);
                return false;
            }
        });
    }

    default Seq<IntPair<T>> withInt(ToIntFunction<T> function) {
        return map(t -> new IntPair<>(function.applyAsInt(t), t));
    }

    default Seq<DoublePair<T>> withDouble(ToDoubleFunction<T> function) {
        return map(t -> new DoublePair<>(function.applyAsDouble(t), t));
    }

    default Seq<LongPair<T>> withLong(ToLongFunction<T> function) {
        return map(t -> new LongPair<>(function.applyAsLong(t), t));
    }

    default Seq<BoolPair<T>> withBool(Predicate<T> function) {
        return map(t -> new BoolPair<>(function.test(t), t));
    }

    default Seq<IntPair<T>> withIndex() {
        return p -> untilIndexed((i, t) -> p.test(new IntPair<>(i, t)));
    }

    default Seq<T> zip(T t) {
        return p -> until(o -> p.test(o) || p.test(t));
    }

    default <E, R> Seq<R> zipBy(Iterable<E> iterable, BiFunction<T, E, R> function) {
        return zip(iterable).map(function);
    }

    default <K, V> Seq2<K, V> mapIf2(BiPredicate<BiPredicate<K, V>, T> predicate) {
        return p -> until(t -> predicate.test(p, t));
    }

    default <E> Seq2<E, T> pairBy(Function<T, E> function) {
        return p -> until(t -> p.test(function.apply(t), t));
    }

    default <E> Seq2<T, E> pairWith(Function<T, E> function) {
        return p -> until(t -> p.test(t, function.apply(t)));
    }

    default Seq2<T, T> toPairs(boolean overlapping) {
        return p -> until(new Predicate<T>() {
            boolean flag;
            T last = null;

            @Override
            public boolean test(T t) {
                if (flag && p.test(last, t)) {
                    return true;
                }
                flag = overlapping || !flag;
                last = t;
                return false;
            }
        });
    }

    default <E> Seq2<T, E> zip(Iterable<E> iterable) {
        return p -> {
            Iterator<E> iterator = iterable.iterator();
            return until(t -> !iterator.hasNext() || p.test(t, iterator.next()));
        };
    }

    default SeqList<T> reverse() {
        return reduce(Reducer.reverse());
    }

    default <E extends Comparable<E>> SeqList<T> sortBy(Function<T, E> function) {
        return sortWith(Comparator.comparing(function));
    }

    default <E extends Comparable<E>> SeqList<T> sortByDesc(Function<T, E> function) {
        return sortWith(Comparator.comparing(function).reversed());
    }

    default SeqList<T> sortWith(Comparator<T> comparator) {
        SeqList<T> list = toList();
        list.sort(comparator);
        return list;
    }

    default SeqList<T> sortWithDesc(Comparator<T> comparator) {
        return sortWith(comparator.reversed());
    }

    default SeqList<T> sorted() {
        return sortWith(null);
    }

    default SeqList<T> sortedDesc() {
        return sortWith(Collections.reverseOrder());
    }

    default SeqList<T> toList() {
        return reduce(new SeqList<>(sizeOrDefault()), SeqList::add);
    }

    default <K> SeqMap<K, SeqList<T>> groupBy(Function<T, K> toKey) {
        return reduce(Reducer.groupBy(toKey, Reducer.toList()));
    }

    default <K> SeqMap<K, T> groupBy(Function<T, K> toKey, BinaryOperator<T> operator) {
        return reduce(Reducer.groupBy(toKey, Reducer.fold(operator)));
    }

    default <K, E> SeqMap<K, SeqList<E>> groupBy(Function<T, K> toKey, Function<T, E> toValue) {
        return groupBy(toKey, Reducer.mapping(toValue));
    }

    default <K, V> SeqMap<K, V> groupBy(Function<T, K> toKey, Reducer<T, V> reducer) {
        return reduce(Reducer.groupBy(toKey, reducer));
    }

    default <K, V> SeqMap<K, V> toMap(Function<T, K> toKey, Function<T, V> toValue) {
        return reduce(Reducer.toMap(() -> new SeqMap<>(sizeOrDefault()), toKey, toValue));
    }

    default <K> SeqMap<K, T> toMapBy(Function<T, K> toKey) {
        return toMap(toKey, v -> v);
    }

    default <V> SeqMap<T, V> toMapWith(Function<T, V> toValue) {
        return toMap(k -> k, toValue);
    }

    default SeqSet<T> toSet() {
        return reduce(Reducer.toSet(sizeOrDefault()));
    }

    default SizedSeq<T> cache() {
        return toBatched();
    }

    default String join(String sep) {
        return join(sep, Object::toString);
    }

    default String join(String sep, Function<T, String> function) {
        return reduce(Reducer.join(sep, function));
    }

    default T first() {
        return reduce(Reducer.first());
    }

    default T last() {
        return reduce(Reducer.last());
    }

    default T max(Comparator<T> comparator) {
        return reduce(Reducer.max(comparator));
    }

    default T min(Comparator<T> comparator) {
        return reduce(Reducer.min(comparator));
    }

    default T reduce(BinaryOperator<T> binaryOperator) {
        return reduce(Reducer.fold(binaryOperator));
    }

    default T[] toObjArray(IntFunction<T[]> initializer) {
        SizedSeq<T> ts = cache();
        T[] a = initializer.apply(ts.size());
        ts.consumeIndexed((i, t) -> a[i] = t);
        return a;
    }

    default boolean matchAll(Predicate<T> predicate) {
        return !matchAny(predicate.negate());
    }

    default boolean matchAny(Predicate<T> predicate) {
        return find(predicate).isPresent();
    }

    default boolean matchAnyNot(Predicate<T> predicate) {
        return matchAny(predicate.negate());
    }

    default boolean matchNone(Predicate<T> predicate) {
        return !matchAny(predicate);
    }

    default boolean untilIndexed(IntObjPredicate<T> predicate) {
        return until(new Predicate<T>() {
            int index = 0;

            @Override
            public boolean test(T t) {
                return predicate.test(index++, t);
            }
        });
    }

    default boolean[] toBooleanArray(Predicate<T> function) {
        SizedSeq<T> ts = cache();
        boolean[] a = new boolean[ts.size()];
        ts.consumeIndexed((i, t) -> a[i] = function.test(t));
        return a;
    }

    default double average(ToDoubleFunction<T> function) {
        return average(function, null);
    }

    default double average(ToDoubleFunction<T> function, ToDoubleFunction<T> weightFunction) {
        return reduce(Reducer.average(function, weightFunction));
    }

    default double sum(ToDoubleFunction<T> function) {
        return reduce(Reducer.sum(function));
    }

    default double[] toDoubleArray(ToDoubleFunction<T> function) {
        SizedSeq<T> ts = cache();
        double[] a = new double[ts.size()];
        ts.consumeIndexed((i, t) -> a[i] = function.applyAsDouble(t));
        return a;
    }

    default int count() {
        return reduce(Reducer.count());
    }

    default int count(Predicate<T> predicate) {
        return reduce(Reducer.count(predicate));
    }

    default int countNot(Predicate<T> predicate) {
        return reduce(Reducer.count(predicate.negate()));
    }

    default int sizeOrDefault() {
        return 10;
    }

    default int sumInt(ToIntFunction<T> function) {
        return reduce(Reducer.sumInt(function));
    }

    default int[] toIntArray(ToIntFunction<T> function) {
        SizedSeq<T> ts = cache();
        int[] a = new int[ts.size()];
        ts.consumeIndexed((i, t) -> a[i] = function.applyAsInt(t));
        return a;
    }

    default long sumLong(ToLongFunction<T> function) {
        return reduce(Reducer.sumLong(function));
    }

    default long[] toLongArray(ToLongFunction<T> function) {
        SizedSeq<T> ts = cache();
        long[] a = new long[ts.size()];
        ts.consumeIndexed((i, t) -> a[i] = function.applyAsLong(t));
        return a;
    }

    interface IntObjConsumer<T> {
        void accept(int i, T t);
    }

    interface IntObjFunction<T, E> {
        E apply(int i, T t);
    }

    interface IntObjPredicate<T> {
        boolean test(int i, T t);
    }
}

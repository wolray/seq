package com.github.wolray.seq;

import java.util.*;
import java.util.function.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author wolray
 */
@FunctionalInterface
public interface Seq<T> {
    boolean any(Predicate<T> predicate);

    static <T> Seq<T> empty() {
        return p -> false;
    }

    static <T> Seq<T> flat(Seq<Optional<T>> seq) {
        return p -> seq.any(o -> o.filter(p).isPresent());
    }

    @SafeVarargs
    static <T> Seq<T> flat(Seq<T>... seq) {
        return p -> {
            for (Seq<T> s : seq) {
                if (s.any(p)) {
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

    static <T> Seq<T> of(Iterable<T> iterable) {
        return p -> {
            for (T t : iterable) {
                if (p.test(t)) {
                    return true;
                }
            }
            return false;
        };
    }

    @SafeVarargs
    static <T> Seq<T> of(T... ts) {
        return of(Arrays.asList(ts));
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
        any(t -> {
            consumer.accept(t);
            return false;
        });
    }

    default void consumeIndexed(IntObjConsumer<T> consumer) {
        any(new Predicate<T>() {
            int index = 0;

            @Override
            public boolean test(T t) {
                consumer.accept(index++, t);
                return false;
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
        any(reducer);
        return reducer.result();
    }

    default <E> E reduce(E des, BiConsumer<E, T> accumulator) {
        any(t -> {
            accumulator.accept(des, t);
            return false;
        });
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

    default Optional<T> findNot(Predicate<T> predicate) {
        return find(predicate.negate());
    }

    default Optional<T> firstMaybe() {
        return find(t -> true);
    }

    default Optional<T> lastMaybe() {
        Mutable<T> m = new Mutable<>(null);
        consume(m::set);
        return m.toOptional();
    }

    default <V extends Comparable<V>> Pair<T, V> maxBy(Function<T, V> function) {
        return reduce(Reducer.maxBy(function));
    }

    default <V extends Comparable<V>> Pair<T, V> minBy(Function<T, V> function) {
        return reduce(Reducer.minBy(function));
    }

    default Seq<SeqList<T>> chunked(int size) {
        return chunked(size, () -> Reducer.toList(size));
    }

    default <V> Seq<V> chunked(int size, Supplier<Reducer<T, V>> factory) {
        return downstream(Downstream.chunked(size, factory));
    }

    default Seq<T> distinct() {
        return downstream(Downstream.distinct());
    }

    default <E> Seq<T> distinctBy(Function<T, E> function) {
        return downstream(Downstream.distinctBy(function));
    }

    default <E> Seq<E> downstream(Downstream<T, E> downstream) {
        return p -> any(downstream.apply(p));
    }

    default <E> Seq<E> downstream(Downstream.Staged<T, E> downstream) {
        return p -> {
            Downstream.StagedPredicate<T> origin = downstream.apply(p);
            return any(origin) || origin.after();
        };
    }

    default Seq<T> drop(int n) {
        return n <= 0 ? this : downstream(Downstream.drop(n));
    }

    default Seq<T> dropWhile(Predicate<T> predicate) {
        return downstream(Downstream.dropWhile(predicate));
    }

    default Seq<T> duplicateAll(int times) {
        return p -> {
            for (int i = 0; i < times; i++) {
                if (any(p)) {
                    return true;
                }
            }
            return false;
        };
    }

    default Seq<T> duplicateEach(int times) {
        return downstream(Downstream.duplicateEach(times));
    }

    default Seq<T> duplicateIf(int times, Predicate<T> predicate) {
        return downstream(Downstream.duplicateIf(times, predicate));
    }

    default Seq<T> filter(Predicate<T> predicate) {
        return downstream(Downstream.filter(predicate));
    }

    default Seq<T> filterIn(Collection<T> collection) {
        return filter(collection::contains);
    }

    default Seq<T> filterIn(Map<T, ?> map) {
        return filter(map::containsKey);
    }

    default Seq<T> filterIndexed(IntObjPredicate<T> predicate) {
        return downstream(Downstream.filterIndexed(predicate));
    }

    default <E extends T> Seq<E> filterInstance(Class<E> cls) {
        return downstream(Downstream.filterInstance(cls));
    }

    default Seq<T> filterNot(Predicate<T> predicate) {
        return downstream(Downstream.filterNot(predicate));
    }

    default Seq<T> filterNotIn(Collection<T> collection) {
        return filterNot(collection::contains);
    }

    default Seq<T> filterNotIn(Map<T, ?> map) {
        return filterNot(map::containsKey);
    }

    default Seq<T> filterNotNull() {
        return downstream(Downstream.filterNotNull());
    }

    default <E> Seq<E> flatIterable(Function<T, Iterable<E>> function) {
        return downstream(Downstream.flatIterable(function));
    }

    default <E> Seq<E> flatMap(Function<T, Seq<E>> function) {
        return downstream(Downstream.flatMap(function));
    }

    default <E> Seq<E> flatOptional(Function<T, Optional<E>> function) {
        return downstream(Downstream.flatOptional(function));
    }

    default <E> Seq<E> map(Function<T, E> function) {
        return downstream(Downstream.map(function));
    }

    default <E> Seq<E> mapIndexed(IntObjFunction<T, E> function) {
        return downstream(Downstream.mapIndexed(function));
    }

    default Seq<String> mapStr() {
        return downstream(Downstream.map(Objects::toString));
    }

    default Seq<T> onEach(Consumer<T> consumer) {
        return downstream(Downstream.onEach(consumer));
    }

    default Seq<T> onEachIndexed(IntObjConsumer<T> consumer) {
        return downstream(Downstream.onEachIndexed(consumer));
    }

    default Seq<T> partial(int n, Downstream<T, T> downstream) {
        return n <= 0 ? this : downstream(Downstream.partial(n, downstream));
    }

    default Seq<T> replace(int n, UnaryOperator<T> operator) {
        return partial(n, Downstream.map(operator));
    }

    default <E> Seq<E> runningFold(E init, BiFunction<E, T, E> function) {
        return downstream(Downstream.runningFold(init, function));
    }

    default <E extends Comparable<E>> Seq<T> sortCached(Function<T, E> function) {
        return map(t -> new Pair<>(t, function.apply(t))).sortBy(p -> p.second).map(p -> p.first);
    }

    default <E extends Comparable<E>> Seq<T> sortCachedDesc(Function<T, E> function) {
        return map(t -> new Pair<>(t, function.apply(t))).sortByDesc(p -> p.second).map(p -> p.first);
    }

    default Seq<T> take(int n) {
        return n <= 0 ? empty() : downstream(Downstream.take(n));
    }

    default Seq<T> takeWhile(BiPredicate<T, T> testPrevCurr) {
        return downstream(Downstream.takeWhile(testPrevCurr));
    }

    default Seq<T> takeWhile(Predicate<T> predicate) {
        return downstream(Downstream.takeWhile(predicate));
    }

    default Seq<T> takeWhileEquals() {
        return takeWhile(Objects::equals);
    }

    default Seq<T> timeLimit(long millis) {
        return millis <= 0 ? this : downstream(Downstream.timeLimit(millis));
    }

    default Seq<T> union(Iterable<T> iterable) {
        return downstream(Downstream.union(iterable));
    }

    default Seq<T> union(T t) {
        return downstream(Downstream.union(t));
    }

    @SuppressWarnings("unchecked")
    default Seq<T> union(T... t) {
        return union(Arrays.asList(t));
    }

    default Seq<T> unionAll(Seq<T> seq) {
        return p -> any(p) || seq.any(p);
    }

    default <V> Seq<V> windowed(int size, int step, boolean allowPartial, Supplier<Reducer<T, V>> factory) {
        return downstream(Downstream.windowed(size, step, allowPartial, factory));
    }

    default <V> Seq<V> windowedByTime(long timeMillis, Supplier<Reducer<T, V>> factory) {
        return downstream(Downstream.windowedByTime(timeMillis, factory));
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
        return downstream(Downstream.withIndex());
    }

    default Seq<T> zip(T t) {
        return downstream(Downstream.zip(t));
    }

    default <E, R> Seq<R> zipBy(Iterable<E> iterable, BiFunction<T, E, R> function) {
        return zip(iterable).map(function);
    }

    default <K, V> Seq2<K, V> downstream2(Function<BiPredicate<K, V>, Predicate<T>> function) {
        return p -> any(function.apply(p));
    }

    default <E> Seq2<E, T> pairBy(Function<T, E> function) {
        return p -> any(t -> p.test(function.apply(t), t));
    }

    default <E> Seq2<T, E> pairWith(Function<T, E> function) {
        return p -> any(t -> p.test(t, function.apply(t)));
    }

    default Seq2<T, T> toPairs(boolean overlapping) {
        return p -> any(new Predicate<T>() {
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
            return any(t -> !iterator.hasNext() || p.test(t, iterator.next()));
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

    default <E> SeqList<E> toList(Function<T, E> function) {
        return reduce(new SeqList<>(sizeOrDefault()), (ls, t) -> ls.add(function.apply(t)));
    }

    default <K> SeqMap<K, SeqList<T>> groupBy(Function<T, K> toKey) {
        return reduce(Reducer.groupBy(toKey, Reducer::toList));
    }

    default <K> SeqMap<K, T> groupBy(Function<T, K> toKey, BinaryOperator<T> operator) {
        return reduce(Reducer.groupBy(toKey, () -> Reducer.fold(operator)));
    }

    default <K, E> SeqMap<K, SeqList<E>> groupBy(Function<T, K> toKey, Function<T, E> toValue) {
        return groupBy(toKey, () -> Reducer.mapping(toValue));
    }

    default <K, V> SeqMap<K, V> groupBy(Function<T, K> toKey, Supplier<Reducer<T, V>> factory) {
        return reduce(Reducer.groupBy(toKey, factory));
    }

    default <K, V> SeqMap<K, V> toMap(BiConsumer<SeqMap<K, V>, T> consumer) {
        return reduce(new SeqMap<>(sizeOrDefault()), consumer);
    }

    default SeqSet<T> toSet() {
        return reduce(Reducer.toSet(sizeOrDefault()));
    }

    default <E> SeqSet<E> toSet(Function<T, E> function) {
        return reduce(new SeqSet<>(sizeOrDefault()), (ls, t) -> ls.add(function.apply(t)));
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

    default boolean all(Predicate<T> predicate) {
        return !any(predicate.negate());
    }

    default boolean none(Predicate<T> predicate) {
        return !any(predicate);
    }

    default boolean untilIndexed(IntObjPredicate<T> predicate) {
        return any(new Predicate<T>() {
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

    default double average(BiConsumer<AverageFolder, T> consumer) {
        return reduce(Reducer.average(consumer));
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
}

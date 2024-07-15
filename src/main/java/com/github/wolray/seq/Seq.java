package com.github.wolray.seq;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author wolray
 */
public interface Seq<T> extends Seq0<Consumer<T>> {
    @SuppressWarnings("unchecked")
    static <T> Seq<T> empty() {
        return (Seq<T>)Empty.emptySeq;
    }

    static <T> Seq<T> flat(Seq<Optional<T>> optionalSeq) {
        return c -> optionalSeq.consume(o -> o.ifPresent(c));
    }

    @SafeVarargs
    static <T> Seq<T> flat(Seq<T>... seq) {
        return c -> {
            for (Seq<T> s : seq) {
                s.consume(c);
            }
        };
    }

    static <T> ItrSeq<T> flatIterable(Iterable<Optional<T>> iterable) {
        return () -> ItrUtil.flatOptional(iterable.iterator());
    }

    @SafeVarargs
    static <T> ItrSeq<T> flatIterable(Iterable<T>... iterables) {
        return () -> ItrUtil.flat(Arrays.asList(iterables).iterator());
    }

    static <T> ItrSeq<T> gen(Supplier<T> supplier) {
        return () -> new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public T next() {
                return supplier.get();
            }
        };
    }

    static <T> Seq<T> gen(T seed, UnaryOperator<T> operator) {
        return c -> {
            T t = seed;
            c.accept(t);
            while (true) {
                c.accept(t = operator.apply(t));
            }
        };
    }

    static <T> Seq<T> gen(T seed1, T seed2, BinaryOperator<T> operator) {
        return c -> {
            T t1 = seed1, t2 = seed2;
            c.accept(t1);
            c.accept(t2);
            while (true) {
                c.accept(t2 = operator.apply(t1, t1 = t2));
            }
        };
    }

    static ItrSeq<Matcher> match(String s, Pattern pattern) {
        return () -> new Iterator<Matcher>() {
            Matcher matcher = pattern.matcher(s);

            @Override
            public boolean hasNext() {
                return matcher.find();
            }

            @Override
            public Matcher next() {
                return matcher;
            }
        };
    }

    @SuppressWarnings("unchecked")
    static <T> Consumer<T> nothing() {
        return (Consumer<T>)Empty.nothing;
    }

    static <T> Seq<T> of(Iterable<T> iterable) {
        return iterable instanceof ItrSeq ? (ItrSeq<T>)iterable : (ItrSeq<T>)iterable::iterator;
    }

    static <K, V> SeqMap<K, V> of(Map<K, V> map) {
        return SeqMap.of(map);
    }

    static <T> Seq<T> of(Optional<T> optional) {
        return optional::ifPresent;
    }

    @SafeVarargs
    static <T> Seq<T> of(T... ts) {
        return of(Arrays.asList(ts));
    }

    static Seq<Object> ofJson(Object node) {
        return Seq.ofTree(node, n -> c -> {
            if (n instanceof Iterable) {
                ((Iterable<?>)n).forEach(c);
            } else if (n instanceof Map) {
                ((Map<?, ?>)n).values().forEach(c);
            }
        });
    }

    static <N> Seq<N> ofTree(N node, Function<N, Seq<N>> sub) {
        return SeqExpand.of(sub).toSeq(node);
    }

    static <N> Seq<N> ofTree(int maxDepth, N node, Function<N, Seq<N>> sub) {
        return SeqExpand.of(sub).toSeq(node, maxDepth);
    }

    static <T> ItrSeq<T> repeat(int n, T t) {
        return () -> new Iterator<T>() {
            int i = n;

            @Override
            public boolean hasNext() {
                return i > 0;
            }

            @Override
            public T next() {
                i--;
                return t;
            }
        };
    }

    static <T> T stop() {
        throw StopException.INSTANCE;
    }

    static <T> ItrSeq<T> tillNull(Supplier<T> supplier) {
        return () -> new PickItr<T>() {
            @Override
            public T pick() {
                T t = supplier.get();
                return t != null ? t : Seq.stop();
            }
        };
    }

    static <T> Seq<T> unit(T t) {
        return c -> c.accept(t);
    }

    default boolean all(Predicate<T> predicate) {
        return !find(predicate.negate()).isPresent();
    }

    default boolean any(Predicate<T> predicate) {
        return find(predicate).isPresent();
    }

    default boolean anyNot(Predicate<T> predicate) {
        return any(predicate.negate());
    }

    default Seq<T> append(T t) {
        return c -> {
            consume(c);
            c.accept(t);
        };
    }

    @SuppressWarnings("unchecked")
    default Seq<T> append(T... t) {
        return c -> {
            consume(c);
            for (T x : t) {
                c.accept(x);
            }
        };
    }

    default Seq<T> appendAll(Iterable<T> iterable) {
        return c -> {
            consume(c);
            iterable.forEach(c);
        };
    }

    default Seq<T> appendWith(Seq<T> seq) {
        return c -> {
            consume(c);
            seq.consume(c);
        };
    }

    default ItrSeq<T> asIterable() {
        return toBatched();
    }

    default double average(ToDoubleFunction<T> function) {
        return average(function, null);
    }

    default double average(ToDoubleFunction<T> function, ToDoubleFunction<T> weightFunction) {
        return reduce(Reducer.average(function, weightFunction));
    }

    default SizedSeq<T> cache() {
        return toBatched();
    }

    default Seq<ArraySeq<T>> chunked(int size) {
        return chunked(size, Reducer.toList(size));
    }

    default <V> Seq<V> chunked(int size, Reducer<T, V> reducer) {
        if (size <= 0) {
            throw new IllegalArgumentException("non-positive size");
        }
        Supplier<V> supplier = reducer.supplier();
        BiConsumer<V, T> accumulator = reducer.accumulator();
        Consumer<V> finisher = reducer.finisher();
        return c -> {
            IntPair<V> intPair = new IntPair<>(0, supplier.get());
            reduce(intPair, (p, t) -> {
                if (p.intVal == size) {
                    if (finisher != null) {
                        finisher.accept(p.it);
                    }
                    c.accept(p.it);
                    p.it = supplier.get();
                    p.intVal = 0;
                }
                accumulator.accept(p.it, t);
                p.intVal++;
            });
            if (intPair.it != null) {
                c.accept(intPair.it);
            }
        };
    }

    default <V, E> Seq<E> chunked(int size, Transducer<T, V, E> transducer) {
        return chunked(size, transducer.reducer()).map(transducer.transformer());
    }

    default Seq<T> circle() {
        return c -> {
            while (true) {
                consume(c);
            }
        };
    }

    default <C extends Collection<T>> C collectBy(IntFunction<C> constructor) {
        return reduce(constructor.apply(sizeOrDefault()), Collection::add);
    }

    default void consume(Consumer<T> consumer, int n, Consumer<T> substitute) {
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

    default void consumeIndexed(IndexObjConsumer<T> consumer) {
        int[] a = {0};
        consume(t -> consumer.accept(a[0]++, t));
    }

    default void consumeIndexedTillStop(IndexObjConsumer<T> consumer) {
        int[] a = {0};
        consumeTillStop(t -> consumer.accept(a[0]++, t));
    }

    default int count() {
        return reduce(Reducer.count());
    }

    default int count(Predicate<T> predicate) {
        return reduce(Reducer.count(predicate));
    }

    default int countNot(Predicate<T> predicate) {
        return reduce(Reducer.countNot(predicate));
    }

    default Seq<T> distinct() {
        return c -> reduce(new HashSet<>(), (set, t) -> {
            if (set.add(t)) {
                c.accept(t);
            }
        });
    }

    default <E> Seq<T> distinctBy(Function<T, E> function) {
        return c -> reduce(new HashSet<>(), (set, t) -> {
            if (set.add(function.apply(t))) {
                c.accept(t);
            }
        });
    }

    default Seq<T> drop(int n) {
        return n <= 0 ? this : partial(n, nothing());
    }

    default Seq<T> dropWhile(Predicate<T> predicate) {
        return c -> foldBoolean(false, (b, t) -> {
            if (b || !predicate.test(t)) {
                c.accept(t);
                return true;
            }
            return false;
        });
    }

    default Seq<T> duplicateAll(int times) {
        return c -> {
            for (int i = 0; i < times; i++) {
                consume(c);
            }
        };
    }

    default Seq<T> duplicateEach(int times) {
        return c -> consume(t -> {
            for (int i = 0; i < times; i++) {
                c.accept(t);
            }
        });
    }

    default Seq<T> duplicateIf(int times, Predicate<T> predicate) {
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

    default Seq<T> filter(Predicate<T> predicate) {
        return predicate == null ? this : c -> consume(t -> {
            if (predicate.test(t)) {
                c.accept(t);
            }
        });
    }

    default Seq<T> filter(int n, Predicate<T> predicate) {
        return predicate == null ? this : c -> consume(c, n, t -> {
            if (predicate.test(t)) {
                c.accept(t);
            }
        });
    }

    default Seq<T> filterIn(Collection<T> collection) {
        return collection == null ? this : filter(collection::contains);
    }

    default Seq<T> filterIn(Map<T, ?> map) {
        return map == null ? this : filter(map::containsKey);
    }

    default Seq<T> filterIndexed(IndexObjPredicate<T> predicate) {
        return predicate == null ? this : c -> consumeIndexed((i, t) -> {
            if (predicate.test(i, t)) {
                c.accept(t);
            }
        });
    }

    default <E> Seq<E> filterInstance(Class<E> cls) {
        return c -> consume(t -> {
            if (cls.isInstance(t)) {
                c.accept(cls.cast(t));
            }
        });
    }

    default Seq<T> filterNot(Predicate<T> predicate) {
        return predicate == null ? this : filter(predicate.negate());
    }

    default Seq<T> filterNotIn(Collection<T> collection) {
        return collection == null ? this : filterNot(collection::contains);
    }

    default Seq<T> filterNotIn(Map<T, ?> map) {
        return map == null ? this : filterNot(map::containsKey);
    }

    default Seq<T> filterNotNull() {
        return filter(Objects::nonNull);
    }

    default Optional<T> find(Predicate<T> predicate) {
        Mutable<T> m = new Mutable<>(null);
        consumeTillStop(t -> {
            if (predicate.test(t)) {
                m.set(t);
                stop();
            }
        });
        return m.toOptional();
    }

    default Optional<T> findDuplicate() {
        Set<T> set = new HashSet<>(sizeOrDefault());
        return find(t -> !set.add(t));
    }

    default Optional<T> findNot(Predicate<T> predicate) {
        return find(predicate.negate());
    }

    default T first() {
        Mutable<T> m = new Mutable<>(null);
        consumeTillStop(t -> {
            m.it = t;
            stop();
        });
        return m.it;
    }

    default Optional<T> firstMaybe() {
        return find(t -> true);
    }

    default <E> Seq<E> flatIterable(Function<T, Iterable<E>> function) {
        return c -> consume(t -> function.apply(t).forEach(c));
    }

    default <E> Seq<E> flatMap(Function<T, Seq<E>> function) {
        return c -> consume(t -> function.apply(t).consume(c));
    }

    default <E> Seq<E> flatOptional(Function<T, Optional<E>> function) {
        return c -> consume(t -> function.apply(t).ifPresent(c));
    }

    default <E> E fold(E init, BiFunction<E, T, E> function) {
        Mutable<E> m = new Mutable<>(init);
        consume(t -> m.it = function.apply(m.it, t));
        return m.it;
    }

    default int foldInt(int init, IntObjToInt<T> function) {
        int[] a = {init};
        consume(t -> a[0] = function.apply(a[0], t));
        return a[0];
    }

    default double foldDouble(double init, DoubleObjToDouble<T> function) {
        double[] a = {init};
        consume(t -> a[0] = function.apply(a[0], t));
        return a[0];
    }

    default long foldLong(long init, LongObjToLong<T> function) {
        long[] a = {init};
        consume(t -> a[0] = function.apply(a[0], t));
        return a[0];
    }

    default boolean foldBoolean(boolean init, BooleanObjToBoolean<T> function) {
        boolean[] a = {init};
        consume(t -> a[0] = function.apply(a[0], t));
        return a[0];
    }

    default <E> E foldAtomic(E init, BiFunction<E, T, E> function) {
        AtomicReference<E> m = new AtomicReference<>(init);
        consume(t -> m.updateAndGet(e -> function.apply(e, t)));
        return m.get();
    }

    default <K> SeqMap<K, ArraySeq<T>> groupBy(Function<T, K> toKey) {
        return groupBy(toKey, Reducer.toList());
    }

    default <K> SeqMap<K, T> groupBy(Function<T, K> toKey, BinaryOperator<T> operator) {
        return groupBy(toKey, Transducer.of(operator));
    }

    default <K, V> SeqMap<K, V> groupBy(Function<T, K> toKey, Reducer<T, V> reducer) {
        return reduce(Reducer.groupBy(toKey, reducer));
    }

    default <K, E> SeqMap<K, ArraySeq<E>> groupBy(Function<T, K> toKey, Function<T, E> toValue) {
        return groupBy(toKey, Reducer.mapping(toValue));
    }

    default <K, V, E> SeqMap<K, E> groupBy(Function<T, K> toKey, Transducer<T, V, E> transducer) {
        return reduce(Reducer.groupBy(toKey, transducer));
    }

    default String join(String sep) {
        return join(sep, Object::toString);
    }

    default String join(String sep, Function<T, String> function) {
        return reduce(new StringJoiner(sep), (j, t) -> j.add(function.apply(t))).toString();
    }

    default T last() {
        return reduce(new Mutable<T>(null), Mutable::set).it;
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

    default Lazy<T> lazyLast() {
        return new Mutable<T>(null) {
            @Override
            protected void eval() {
                consume(t -> it = t);
            }
        };
    }

    default Lazy<T> lazyReduce(BinaryOperator<T> binaryOperator) {
        return Lazy.of(() -> reduce(binaryOperator));
    }

    default <E> Lazy<E> lazyReduce(Reducer<T, E> reducer) {
        return Lazy.of(() -> reduce(reducer));
    }

    default <E, V> Lazy<E> lazyReduce(Transducer<T, V, E> transducer) {
        return Lazy.of(() -> reduce(transducer));
    }

    default <E> Seq<E> map(Function<T, E> function) {
        return c -> consume(t -> c.accept(function.apply(t)));
    }

    default <E> Seq<E> map(Function<T, E> function, int n, Function<T, E> substitute) {
        return n <= 0 ? map(function) : c -> {
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

    default <E> Seq<E> mapIndexed(IndexObjFunction<T, E> function) {
        return c -> consumeIndexed((i, t) -> c.accept(function.apply(i, t)));
    }

    default <E> Seq<E> mapMaybe(Function<T, E> function) {
        return c -> consume(t -> {
            if (t != null) {
                c.accept(function.apply(t));
            }
        });
    }

    default <E> Seq<E> mapNotNull(Function<T, E> function) {
        return c -> consume(t -> {
            E e = function.apply(t);
            if (e != null) {
                c.accept(e);
            }
        });
    }

    default Seq2<T, T> mapPair(boolean overlapping) {
        return c -> reduce(new BoolPair<>(false, (T)null), (p, t) -> {
            if (p.flag) {
                c.accept(p.it, t);
            }
            p.flag = overlapping || !p.flag;
            p.it = t;
        });
    }

    default Seq<ArraySeq<T>> mapSub(Predicate<T> takeWhile) {
        return mapSub(takeWhile, Reducer.toList());
    }

    default Seq<ArraySeq<T>> mapSub(T first, T last) {
        return mapSub(first, last, Reducer.toList());
    }

    default <V> Seq<V> mapSub(Predicate<T> takeWhile, Reducer<T, V> reducer) {
        Supplier<V> supplier = reducer.supplier();
        BiConsumer<V, T> accumulator = reducer.accumulator();
        return c -> {
            V last = fold(null, (v, t) -> {
                if (takeWhile.test(t)) {
                    if (v == null) {
                        v = supplier.get();
                    }
                    accumulator.accept(v, t);
                    return v;
                } else {
                    if (v != null) {
                        c.accept(v);
                    }
                    return null;
                }
            });
            if (last != null) {
                c.accept(last);
            }
        };
    }

    default <V> Seq<V> mapSub(Predicate<T> first, Predicate<T> last, Reducer<T, V> reducer) {
        Supplier<V> supplier = reducer.supplier();
        BiConsumer<V, T> accumulator = reducer.accumulator();
        return c -> fold((V)null, (v, t) -> {
            if (v == null && first.test(t)) {
                v = supplier.get();
                accumulator.accept(v, t);
            } else if (v != null) {
                accumulator.accept(v, t);
                if (last.test(t)) {
                    c.accept(v);
                    return null;
                }
            }
            return v;
        });
    }

    default <V> Seq<V> mapSub(T first, T last, Reducer<T, V> reducer) {
        return mapSub(first::equals, last::equals, reducer);
    }

    default IntSeq mapToInt(ToIntFunction<T> function) {
        return c -> consume(t -> c.accept(function.applyAsInt(t)));
    }

    default T max(Comparator<T> comparator) {
        return reduce(Reducer.max(comparator));
    }

    default <V extends Comparable<V>> Pair<T, V> maxBy(Function<T, V> function) {
        return reduce(Reducer.maxBy(function));
    }

    default T min(Comparator<T> comparator) {
        return reduce(Reducer.min(comparator));
    }

    default <V extends Comparable<V>> Pair<T, V> minBy(Function<T, V> function) {
        return reduce(Reducer.minBy(function));
    }

    default boolean none(Predicate<T> predicate) {
        return !find(predicate).isPresent();
    }

    default Seq<T> onEach(Consumer<T> consumer) {
        return c -> consume(consumer.andThen(c));
    }

    default Seq<T> onEach(int n, Consumer<T> consumer) {
        return c -> consume(c, n, consumer.andThen(c));
    }

    default Seq<T> onEachIndexed(IndexObjConsumer<T> consumer) {
        return c -> consumeIndexed((i, t) -> {
            consumer.accept(i, t);
            c.accept(t);
        });
    }

    default <A, B> Seq2<A, B> pair(Function<T, A> f1, Function<T, B> f2) {
        return c -> consume(t -> c.accept(f1.apply(t), f2.apply(t)));
    }

    default <E> Seq2<E, T> pairBy(Function<T, E> function) {
        return c -> consume(t -> c.accept(function.apply(t), t));
    }

    default <E> Seq2<E, T> pairByNotNull(Function<T, E> function) {
        return c -> consume(t -> {
            E e = function.apply(t);
            if (e != null) {
                c.accept(e, t);
            }
        });
    }

    default <E> Seq2<T, E> pairWith(Function<T, E> function) {
        return c -> consume(t -> c.accept(t, function.apply(t)));
    }

    default <E> Seq2<T, E> pairWithNotNull(Function<T, E> function) {
        return c -> consume(t -> {
            E e = function.apply(t);
            if (e != null) {
                c.accept(t, e);
            }
        });
    }

    default Seq<T> parallel() {
        return parallel(Async.common());
    }

    default Seq<T> parallel(Async async) {
        return c -> async.joinAll(map(t -> () -> c.accept(t)));
    }

    default Seq<T> parallelNoJoin() {
        return parallelNoJoin(Async.common());
    }

    default Seq<T> parallelNoJoin(Async async) {
        return c -> consume(t -> async.submit(() -> c.accept(t)));
    }

    default Seq<T> partial(int n, Consumer<T> substitute) {
        return c -> consume(c, n, substitute);
    }

    default void printAll(String sep) {
        if ("\n".equals(sep)) {
            println();
        } else {
            System.out.println(join(sep));
        }
    }

    default void println() {
        consume(System.out::println);
    }

    default T reduce(BinaryOperator<T> binaryOperator) {
        return reduce(Transducer.of(binaryOperator));
    }

    default <E> E reduce(Reducer<T, E> reducer) {
        E des = reducer.supplier().get();
        BiConsumer<E, T> accumulator = reducer.accumulator();
        consume(t -> accumulator.accept(des, t));
        Consumer<E> finisher = reducer.finisher();
        if (finisher != null) {
            finisher.accept(des);
        }
        return des;
    }

    default <E, V> E reduce(Transducer<T, V, E> transducer) {
        return transducer.transformer().apply(reduce(transducer.reducer()));
    }

    default <E> E reduce(E des, BiConsumer<E, T> accumulator) {
        consume(t -> accumulator.accept(des, t));
        return des;
    }

    default <E, V> E reduce(Reducer<T, V> reducer, Function<V, E> transformer) {
        return transformer.apply(reduce(reducer));
    }

    default Seq<T> replace(int n, UnaryOperator<T> operator) {
        return c -> consume(c, n, t -> c.accept(operator.apply(t)));
    }

    default ArraySeq<T> reverse() {
        return reduce(Reducer.reverse());
    }

    default <E> Seq<E> runningFold(E init, BiFunction<E, T, E> function) {
        return c -> fold(init, (e, t) -> {
            e = function.apply(e, t);
            c.accept(e);
            return e;
        });
    }

    default Seq<T> shareIn(int buffer, Async async) {
        return async.toShared(buffer, false, this);
    }

    default Seq<T> shareIn(int buffer, boolean delay, Async async) {
        return async.toShared(buffer, delay, this);
    }

    default int sizeOrDefault() {
        return 10;
    }

    default <E extends Comparable<E>> ArraySeq<T> sortBy(Function<T, E> function) {
        return sortWith(Comparator.comparing(function));
    }

    default <E extends Comparable<E>> ArraySeq<T> sortByDesc(Function<T, E> function) {
        return sortWith(Comparator.comparing(function).reversed());
    }

    default <E extends Comparable<E>> Seq<T> sortCached(Function<T, E> function) {
        return map(t -> new Pair<>(t, function.apply(t)))
            .sortBy(p -> p.second)
            .map(p -> p.first);
    }

    default <E extends Comparable<E>> Seq<T> sortCachedDesc(Function<T, E> function) {
        return map(t -> new Pair<>(t, function.apply(t)))
            .sortByDesc(p -> p.second)
            .map(p -> p.first);
    }

    default ArraySeq<T> sortWith(Comparator<T> comparator) {
        ArraySeq<T> list = toList();
        list.sort(comparator);
        return list;
    }

    default ArraySeq<T> sortWithDesc(Comparator<T> comparator) {
        return sortWith(comparator.reversed());
    }

    default ArraySeq<T> sorted() {
        return sortWith(null);
    }

    default ArraySeq<T> sortedDesc() {
        return sortWith(Collections.reverseOrder());
    }

    default Seq<T> stateIn(boolean delay, Async async) {
        return async.toState(delay, this);
    }

    default double sum(ToDoubleFunction<T> function) {
        return reduce(Reducer.sum(function));
    }

    default int sumInt(ToIntFunction<T> function) {
        return reduce(Reducer.sumInt(function));
    }

    default long sumLong(ToLongFunction<T> function) {
        return reduce(Reducer.sumLong(function));
    }

    default Seq<T> take(int n) {
        return n <= 0 ? empty() : c -> {
            int[] i = {n};
            consumeTillStop(t -> {
                c.accept(t);
                if (--i[0] == 0) {
                    stop();
                }
            });
        };
    }

    default Seq<T> takeWhile(Predicate<T> predicate) {
        return c -> consumeTillStop(t -> {
            if (predicate.test(t)) {
                c.accept(t);
            } else {
                stop();
            }
        });
    }

    default Seq<T> takeWhile(BiPredicate<T, T> testPrevCurr) {
        return takeWhile(t -> t, testPrevCurr);
    }

    default <E> Seq<T> takeWhile(Function<T, E> function, BiPredicate<E, E> testPrevCurr) {
        return c -> {
            Mutable<E> m = new Mutable<>(null);
            consumeTillStop(t -> {
                E curr = function.apply(t);
                if (m.it == null || testPrevCurr.test(m.it, curr)) {
                    c.accept(t);
                    m.it = curr;
                } else {
                    stop();
                }
            });
        };
    }

    default Seq<T> takeWhileEquals() {
        return takeWhile(t -> t, Objects::equals);
    }

    default <E> Seq<T> takeWhileEquals(Function<T, E> function) {
        return takeWhile(function, Objects::equals);
    }

    default Seq<T> timeLimit(long millis) {
        return millis <= 0 ? this : c -> {
            long end = System.currentTimeMillis() + millis;
            consumeTillStop(t -> {
                if (System.currentTimeMillis() > end) {
                    stop();
                }
                c.accept(t);
            });
        };
    }

    default T[] toObjArray(IntFunction<T[]> initializer) {
        SizedSeq<T> ts = cache();
        T[] a = initializer.apply(ts.size());
        ts.consumeIndexed((i, t) -> a[i] = t);
        return a;
    }

    default int[] toIntArray(ToIntFunction<T> function) {
        SizedSeq<T> ts = cache();
        int[] a = new int[ts.size()];
        ts.consumeIndexed((i, t) -> a[i] = function.applyAsInt(t));
        return a;
    }

    default double[] toDoubleArray(ToDoubleFunction<T> function) {
        SizedSeq<T> ts = cache();
        double[] a = new double[ts.size()];
        ts.consumeIndexed((i, t) -> a[i] = function.applyAsDouble(t));
        return a;
    }

    default long[] toLongArray(ToLongFunction<T> function) {
        SizedSeq<T> ts = cache();
        long[] a = new long[ts.size()];
        ts.consumeIndexed((i, t) -> a[i] = function.applyAsLong(t));
        return a;
    }

    default boolean[] toBooleanArray(Predicate<T> function) {
        SizedSeq<T> ts = cache();
        boolean[] a = new boolean[ts.size()];
        ts.consumeIndexed((i, t) -> a[i] = function.test(t));
        return a;
    }

    default BatchedSeq<T> toBatched() {
        return reduce(new BatchedSeq<>(), BatchedSeq::add);
    }

    default ConcurrentSeq<T> toConcurrent() {
        return reduce(new ConcurrentSeq<>(), ConcurrentSeq::add);
    }

    default <E> Lazy<E> toLazy(Reducer<T, E> reducer) {
        return Lazy.of(() -> reduce(reducer));
    }

    default <V, E> Lazy<E> toLazy(Transducer<T, V, E> transducer) {
        return Lazy.of(() -> reduce(transducer));
    }

    default LinkedSeq<T> toLinked() {
        return reduce(new LinkedSeq<>(), LinkedSeq::add);
    }

    default ArraySeq<T> toList() {
        return reduce(new ArraySeq<>(sizeOrDefault()), ArraySeq::add);
    }

    default <K, V> SeqMap<K, V> toMap(Function<T, K> toKey, Function<T, V> toValue) {
        return reduce(Reducer.toMap(() -> new LinkedHashMap<>(sizeOrDefault()), toKey, toValue));
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

    default <A, B, D> Seq3<A, B, D> triple(BiConsumer<Consumer3<A, B, D>, T> consumer) {
        return c -> consume(t -> consumer.accept(c, t));
    }

    default <A, B, D> Seq3<A, B, D> triple(Function<T, A> f1, Function<T, B> f2, Function<T, D> f3) {
        return c -> consume(t -> c.accept(f1.apply(t), f2.apply(t), f3.apply(t)));
    }

    default Seq<ArraySeq<T>> windowed(int size, int step, boolean allowPartial) {
        return windowed(size, step, allowPartial, Reducer.toList());
    }

    default <V> Seq<V> windowed(int size, int step, boolean allowPartial, Reducer<T, V> reducer) {
        if (size <= 0 || step <= 0) {
            throw new IllegalArgumentException("non-positive size or step");
        }
        return c -> {
            Supplier<V> supplier = reducer.supplier();
            BiConsumer<V, T> accumulator = reducer.accumulator();
            Consumer<V> finisher = reducer.finisher();
            Queue<IntPair<V>> queue = new LinkedList<>();
            foldInt(0, (left, t) -> {
                if (left == 0) {
                    left = step;
                    queue.offer(new IntPair<>(0, supplier.get()));
                }
                queue.forEach(sub -> {
                    accumulator.accept(sub.it, t);
                    sub.intVal++;
                });
                IntPair<V> first = queue.peek();
                if (first != null && first.intVal == size) {
                    queue.poll();
                    if (finisher != null) {
                        finisher.accept(first.it);
                    }
                    c.accept(first.it);
                }
                return left - 1;
            });
            if (allowPartial) {
                queue.forEach(p -> c.accept(p.it));
            }
            queue.clear();
        };
    }

    default <V, E> Seq<E> windowed(int size, int step, boolean allowPartial, Transducer<T, V, E> transducer) {
        return windowed(size, step, allowPartial, transducer.reducer()).map(transducer.transformer());
    }

    default Seq<ArraySeq<T>> windowedByTime(long timeMillis) {
        return windowedByTime(timeMillis, Reducer.toList());
    }

    default <V> Seq<V> windowedByTime(long timeMillis, Reducer<T, V> reducer) {
        if (timeMillis <= 0) {
            throw new IllegalArgumentException("non-positive time");
        }
        return c -> {
            Supplier<V> supplier = reducer.supplier();
            BiConsumer<V, T> accumulator = reducer.accumulator();
            Consumer<V> finisher = reducer.finisher();
            reduce(new LongPair<>(System.currentTimeMillis(), supplier.get()), (p, t) -> {
                long now = System.currentTimeMillis();
                if (now - p.longVal > timeMillis) {
                    p.longVal = now;
                    if (finisher != null) {
                        finisher.accept(p.it);
                    }
                    c.accept(p.it);
                    p.it = supplier.get();
                }
                accumulator.accept(p.it, t);
            });
        };
    }

    default Seq<ArraySeq<T>> windowedByTime(long timeMillis, long stepMillis) {
        return windowedByTime(timeMillis, stepMillis, Reducer.toList());
    }

    default <V, E> Seq<E> windowedByTime(long timeMillis, Transducer<T, V, E> transducer) {
        return windowedByTime(timeMillis, transducer.reducer()).map(transducer.transformer());
    }

    default <V> Seq<V> windowedByTime(long timeMillis, long stepMillis, Reducer<T, V> reducer) {
        if (timeMillis <= 0 || stepMillis <= 0) {
            throw new IllegalArgumentException("non-positive time or step");
        }
        return c -> {
            Supplier<V> supplier = reducer.supplier();
            BiConsumer<V, T> accumulator = reducer.accumulator();
            Consumer<V> finisher = reducer.finisher();
            Queue<LongPair<V>> queue = new LinkedList<>();
            long[] last = {System.currentTimeMillis(), 0};
            reduce(last, (a, t) -> {
                if (a[1] <= 0) {
                    a[1] = stepMillis;
                    queue.offer(new LongPair<>(System.currentTimeMillis(), supplier.get()));
                }
                queue.forEach(sub -> accumulator.accept(sub.it, t));
                LongPair<V> first = queue.peek();
                if (first != null && System.currentTimeMillis() - first.longVal > timeMillis) {
                    queue.poll();
                    if (finisher != null) {
                        finisher.accept(first.it);
                    }
                    c.accept(first.it);
                }
                long now = System.currentTimeMillis();
                a[1] -= now - a[0];
                a[0] = now;
            });
            queue.clear();
        };
    }

    default <V, E> Seq<E> windowedByTime(long timeMillis, long stepMillis, Transducer<T, V, E> transducer) {
        return windowedByTime(timeMillis, stepMillis, transducer.reducer()).map(transducer.transformer());
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
        return c -> consumeIndexed((i, t) -> c.accept(new IntPair<>(i, t)));
    }

    default <E> Seq2<T, E> zip(Iterable<E> iterable) {
        return c -> zip(iterable, c);
    }

    default <B, C> Seq3<T, B, C> zip(Iterable<B> bs, Iterable<C> cs) {
        return c -> zip(bs, cs, c);
    }

    default <E> void zip(Iterable<E> iterable, BiConsumer<T, E> consumer) {
        Iterator<E> iterator = iterable.iterator();
        consumeTillStop(t -> consumer.accept(t, ItrUtil.pop(iterator)));
    }

    default <B, C> void zip(Iterable<B> bs, Iterable<C> cs, Consumer3<T, B, C> consumer) {
        Iterator<B> bi = bs.iterator();
        Iterator<C> ci = cs.iterator();
        consumeTillStop(t -> consumer.accept(t, ItrUtil.pop(bi), ItrUtil.pop(ci)));
    }

    interface IntObjToInt<T> {
        int apply(int acc, T t);
    }

    interface DoubleObjToDouble<T> {
        double apply(double acc, T t);
    }

    interface LongObjToLong<T> {
        long apply(long acc, T t);
    }

    interface BooleanObjToBoolean<T> {
        boolean apply(boolean acc, T t);
    }

    interface IndexObjConsumer<T> {
        void accept(int i, T t);
    }

    interface IndexObjFunction<T, E> {
        E apply(int i, T t);
    }

    interface IndexObjPredicate<T> {
        boolean test(int i, T t);
    }

    class Empty {
        static final Seq<Object> emptySeq = c -> {};
        static final Consumer<Object> nothing = t -> {};
    }
}

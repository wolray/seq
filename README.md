# seq
本项目提供一个强大而完备的流式编程API，并独创性的为Java添加类似生成器的编程机制。

网页端参考：[阿里云社区](https://developer.aliyun.com/article/1191351?spm=5176.28261954.J_7341193060.5.44812fdeTRXvK5&scm=20140722.S_community@@%E6%96%87%E7%AB%A0@@1191351._.ID_1191351-RL_%E4%B8%80%E7%A7%8D%E6%96%B0%E7%9A%84%E6%B5%81%E4%B8%BA%20java%20%E5%8A%A0%E5%85%A5%E7%94%9F%E6%88%90%E5%99%A8generator%E7%89%B9%E6%80%A7-LOC_m~UND~search~UND~community~UND~i-OR_ser-V_3-P0_1)

手机端参考：[阿里开发者微信公众号](https://mp.weixin.qq.com/s/v-HMKBWxtz1iakxFL09PDw)

这一机制的设计思想与核心代码在上述公开文章中都已阐述完整，本项目是在此基础上的完全重写，并得到了原作者的鼓励与支持，未来发展也将主要面向开源社区需求。

#### 引用方式
项目已发布到Central Maven，直接引用即可
```xml
<dependency>
    <groupId>io.github.wolray</groupId>
    <artifactId>seq</artifactId>
    <version>1.0.1</version>
</dependency>
```

## 完备的流式API
### 流的定义与种类
#### 一元流
```Java
public interface Seq<T> {
    void consume(Consumer<T> consumer);
}
```
#### 可迭代流ItrSeq
它既是`Iterable`，也是`Seq`，并提供一个额外的默认`zip`方法
```Java
public interface ItrSeq<T> extends Iterable<T>, Seq<T> {
    default ItrSeq<T> zip(T t) {...}
}
```
#### 可数流SizedSeq
它直接继承自`ItrSeq`，同时提供`size`和`isEmpty`方法，以及默认的`isNotEmpty`方法
```Java
public interface SizedSeq<T> extends ItrSeq<T> {
    int size();
    boolean isEmpty();
    default boolean isNotEmpty() {...}
}
```
#### ArrayList流ArraySeq
它直接继承自`ArrayList`，自带它的全部方法和功能，同时也是个`SizedSeq`，该Class是对`ArrayList`的强大扩展，还可以在此基础上追加一些额外方法，例如`swap`
```Java
public class ArraySeq<T> extends ArrayList<T> implements SizedSeq<T> {
    public void swap(int i, int j) {...}
}
```
#### LinkedList流LinkedSeq
它直接继承自`LinkedList`，自带它的全部方法和功能，同时也是个`SizedSeq`
```Java
public class LinkedSeq<T> extends LinkedList<T> implements SizedSeq<T> {...}
```
### 流的创建
#### 基于生成器表达式
```Java
Seq<Integer> seq = c -> {
    c.accept(0);
    int i = 1;
    for (; i < 10; i++) {
        c.accept(i);
    }
    // 这是一个无限流
    while (true) {
        c.accept(i++);
    }
};
```
#### 单位流
```Java
static <T> Seq<T> unit(T t) {...}
```
#### 基于Iterable
```Java
static <T> Seq<T> of(Iterable<T> iterable) {...}
```
#### 基于可变参数
```Java
static <T> Seq<T> of(T... ts) {...}
```
#### 基于Supplier
```Java
static <T> ItrSeq<T> gen(Supplier<T> supplier) {...}
static <T> ItrSeq<T> tillNull(Supplier<T> supplier) {...}
```
#### 基于Optional
```Java
static <T> Seq<T> of(Optional<T> optional) {...}
```
#### 基于正则匹配
```Java
static ItrSeq<Matcher> match(String s, Pattern pattern) {...}
```
#### 基于树
```Java
static <N> Seq<N> ofTree(N node, Function<N, Seq<N>> sub) {...}
// 对分支深度做限制，可用于树搜索算法
static <N> Seq<N> ofTree(int maxDepth, N node, Function<N, Seq<N>> sub) {...}
```
#### 基于JSONObject
```Java
static Seq<Object> ofJson(Object node) {...}
```
#### 基于元素重复
```Java
static <T> ItrSeq<T> repeat(int n, T t) {...}
```
### 流的链式调用
链式调用是流的核心功能，它由一个流触发，返回一个新的流，中间可对数据进行映射、过滤、排序等等各种操作
#### map 流的映射
```Java
default <E> Seq<E> map(Function<T, E> function) {...}
```
其他映射方式
```Java
// 分段映射，前n项使用substitute，其他使用function
default <E> Seq<E> map(Function<T, E> function, int n, Function<T, E> substitute) {...}
// 带下标映射
default <E> Seq<E> mapIndexed(IndexObjFunction<T, E> function) {...}
// 仅对非空元素映射
default <E> Seq<E> mapMaybe(Function<T, E> function) {...}
// 映射后过滤非空元素
default <E> Seq<E> mapNotNull(Function<T, E> function) {...}
```
#### 元素过滤 filter
```Java
default Seq<T> filter(Predicate<T> predicate) {...}
// 以及只在前n项里过滤
default Seq<T> filter(int n, Predicate<T> predicate) {...}
```
其他过滤方法
```Java
//取collection交集
default Seq<T> filterIn(Collection<T> collection) {...}
//取map交集
default Seq<T> filterIn(Map<T, ?> map) {...}
//带下标过滤
default Seq<T> filterIndexed(IndexObjPredicate<T> predicate) {...}
//按类型过滤
default <E> Seq<E> filterInstance(Class<E> cls) {...}
//按条件否过滤
default Seq<T> filterNot(Predicate<T> predicate) {...}
default Seq<T> filterNotIn(Collection<T> collection) {...}
default Seq<T> filterNotIn(Map<T, ?> map) {...}
//非空过滤
default Seq<T> filterNotNull() {...}
```

#### 展平流 flatMap
可以认为是Seq Monad
```Java
default <E> Seq<E> flatMap(Function<T, Seq<E>> function) {...}
```
#### 按Optional展平 flatOptional
```Java
default <E> Seq<E> flatOptional(Function<T, Optional<E>> function) {..
```

#### 处理元素 onEach
处理但不消费
```Java
default Seq<T> onEach(Consumer<T> consumer) {...}
// 只peek前n项
default Seq<T> onEach(int n, Consumer<T> consumer) {...}
// 带下标peek
default Seq<T> onEachIndexed(IndexObjConsumer<T> consumer) {...}
```

#### 流的部分消费 partial
只按照指定方式消费前n项，后面元素保留
```Java
default Seq<T> partial(int n, Consumer<T> substitute) {...}
```

#### 翻转流 reverse
```Java
default ArraySeq<T> reverse() {...}
```

#### 累加流 runningFold
```Java
default <E> Seq<E> runningFold(E init, BiFunction<E, T, E> function) {...}
```
### 流的窗口函数
所谓窗口函数就是对流的元素按照某种规则进行局部聚合，每一个小组聚合为整体后，构成一个新的流。
聚合的逻辑通常有三种，按次数，按时间，按头尾元素特征。
#### 每n个元素分为一组 chunked
```Java
// (1, 1, 2, 2, 2, 3, 4, 4, 5) -> n=3 -> ([1, 1, 2], [2, 2, 3], [4, 4, 5])
default Seq<ArraySeq<T>> chunked(int size) {...}
```
#### 按条件局部分组 mapSub
```Java
// (1, 1, 2, 2, 2, 3, 4, 4, 5) -> isEven -> ([2, 2, 2], [4, 4])
default Seq<ArraySeq<T>> mapSub(Predicate<T> takeWhile) {...}
// (1, 1, 2, 2, 2, 3, 4, 4, 5) -> (isEven, toSet) -> ({2}, {4})
default <V> Seq<V> mapSub(Predicate<T> takeWhile, Reducer<T, V> reducer) {...}
// (1, 1, 2, 2, 2, 3, 4, 4, 5) -> (isOdd, isEven, toList) -> ([1, 1, 2], [3, 4])
default <V> Seq<V> mapSub(Predicate<T> first, Predicate<T> last, Reducer<T, V> reducer) {
```
#### 滑动窗口 windowed
todo

#### 按时间开窗 windowedByTime
需要热流和异步流发布后才能完全发挥价值
todo

### 流的聚合
todo

### 流的分组
todo

## 其他特性

### 惰性有向图与并发递归计算
见文章：[面向上下文(Context)编程：一种带缓存、可并发、惰性递归的编程范式，源码可画图](https://mp.weixin.qq.com/s/lxpoXcH7fGF1_n8iI96d4A)

本项目提供了上文的一种实现。

### 流的缓存
当需要对流进行缓存或者collect时，传统做法是使用`ArrayList`或者`LinkedList`。然而前者需要不时拷贝内部数组，后者每个元素内存成本高，更好的方式是将二者结合，使用扩容后的新数组存放新元素，但不进行数组拷贝，而是直接将新旧数组链接起来。从而既缓存了元素，也实现了内存和性能的双优化。

该实现称之为`BatchedSeq`，已发布，并将应用于各种需要对流进行缓存的场景，包括暂未发布的并发流。


## todo特性
由于内容过多，仍有许多功能尚未实现，暂列于后文。

#### 更好的groupby

#### 二元流/多元流
基于callback机制的二元流，是其独特的衍生特性。二元流最大的优势是不产生任何tuple或pair之类的中间数据结构，即用即走，十分优雅。
Java里的`List`/`Set`/`Iterable`都可以在一元流的基础上实现升级改造，二元流则自然对应`Map`，可以衍生许多有趣玩法。

#### Splitter
Java的`String`提供了默认的`split`实现，但它会将substring收集为一个数组，在关心性能或后续应用的场景下殊为不妥。Google的guava库提供了产出`Iterable`的`Splitter`，十分优秀。

而基于callback机制，我们能实现出更好更快的`Splitter`，相对guava不仅能有20%-40%的性能提升，还可支持任意后续的流式操作。

#### 统一InputStream与数据源
`InputStream`是Java里进行IO交互时一个非常底层且重要的接口，而对于许多常用文件格式，例如CSV/properties，天然一行代表一个数据块。所以`InputStream`和Seq of String是直接对应的。

在另一方面，`InputStream`是一次性的，本身不可重用，从函数式编程的角度来讲，它不算是一种好的数据。所以本项目会对其进行封装，抽象出一个中间数据结构`ISSouce`(意为InputStream Source)。它将对`File`, `Path`, local file, resouce, `URL`以及literal string等常见数据源进行统一收口，实现可重用且IO隔离的安全流式数据源，并提供与`Seq`的互转。

#### 异步流/并发流/异步通道

#### 热流与订阅

## 反馈
可使用中文直接提issue，也可添加微信radiumlei2010进群，原作者也在群里，方便他与大家沟通，收集反馈。

## 发布记录
#### 1.0.1 (20230922)
新增`IntSeq`, `Lazy`, `BatchedSeq`, `SeqSet`, `windowed`, `timeWindowed`

# seq

`seq` 是一个面向 Java 的轻量级流式编程 API。它用函数式接口表达数据流，让普通 Java 代码可以写出类似生成器的生产逻辑，同时支持 `map`、`filter`、`flatMap`、`take`、`drop`、`chunked`、`windowed`、`groupBy`、`reduce` 等常见流处理操作。

`seq` 的设计精妙之处在于，它把流抽象压缩到了一个极小、但表达力很强的协议上：数据源只需要知道如何把元素交给下游，下游只需要用一个布尔返回值告诉上游是否应该停止。这个协议足够简单，简单到任何循环、递归遍历、回调式 API 或临时生成逻辑都可以直接包装成 `Seq`；同时它又足够完整，可以自然承载惰性计算、中间转换、短路终止、状态化下游和终端归约。

相比 `java.util.stream.Stream`，`seq` 更适合描述“边生产、边消费、随时停止”的流程。`Stream` 通常以迭代器或 spliterator 为核心，由终端操作主动拉取元素；`seq` 则让生产者主动推送元素，并把停止信号沿调用栈即时传回生产者。因此，手写生成器、多层循环、树遍历、正则匹配、分页读取、分块窗口等场景不需要额外维护迭代器状态，也不必为了适配拉取模型拆分复杂控制流。

这种推送式模型还带来了一个独特优势：中间操作和下游协议可以保持高度统一。`map`、`filter`、`flatMap` 只是把一个 predicate 包装成另一个 predicate；`chunked`、`windowed` 这类需要收尾的操作可以通过 staged downstream 明确表达生命周期；`Reducer` 也可以复用同一套转换逻辑。最终得到的是一个 API 面很小、组合能力很强的流处理工具，既保留 Java 代码的直接性，又获得接近生成器和协程风格的表达能力。

项目的核心不是基于 `java.util.stream.Stream` 的拉取式模型，而是一个更直接的推送式模型：

```java
@FunctionalInterface
public interface Seq<T> {
    boolean any(Predicate<T> predicate);
}
```

生产者把元素逐个交给 `Predicate<T>`。当 predicate 返回 `true` 时，表示下游已经拿到足够数据，上游应尽快停止。这使得短路操作、生成器、树遍历、正则匹配、分块窗口和自定义流转换都可以用很少的代码表达。

## 安装

项目已发布到 Maven Central：

```xml
<dependency>
    <groupId>io.github.wolray</groupId>
    <artifactId>seq</artifactId>
    <version>2.2.1</version>
</dependency>
```

当前源码以 Java 8 为目标版本编译。

## 快速开始

```java
import com.github.wolray.seq.*;

String result = Seq.of(0, 2, 4, 1, 6, 3, 8)
    .filter(i -> (i & 1) == 0)
    .map(i -> i * 10)
    .take(3)
    .join(",");

// result = "0,20,40"
```

`Seq` 默认是惰性的。上面的 `filter`、`map`、`take` 只是组合流，只有调用 `join`、`toList`、`reduce`、`consume` 等终端操作时才会真正遍历。

## 生成器式写法

`Seq` 本身就是一个函数，因此可以直接用 lambda 创建自定义数据源：

```java
Seq<Integer> cartesian = p -> {
    for (int a : new int[] {10, 20, 30}) {
        for (int b : new int[] {1, 2, 3}) {
            if (p.test(a + b)) {
                return true;
            }
        }
    }
    return false;
};

String firstFour = cartesian.take(4).join(",");

// firstFour = "11,12,13,21"
```

这里的 `p.test(value)` 相当于 `yield value`。如果下游返回 `true`，说明流已经结束，例如 `take(4)` 已经收够 4 个元素，生成器应立即返回。

## 常用流操作

`Seq` 提供了常用的中间操作：

```java
Seq.of(1, 2, null, 3, null, 4)
    .filterNotNull()
    .map(Object::toString)
    .join(",");

Seq.of(1, 1, 2, 3, 3)
    .distinct()
    .toList();

Seq.of(1, 2, 3, 4)
    .runningFold(0, Integer::sum)
    .join(",");

Seq.of(1, 2, 3, 4)
    .duplicateIf(2, i -> (i & 1) == 1)
    .join(",");
```

常见终端操作包括：

```java
Seq.of(1, 2, 3).toList();
Seq.of(1, 2, 3).toSet();
Seq.of(1, 2, 3).first();
Seq.of(1, 2, 3).last();
Seq.of(1, 2, 3).find(i -> i > 1);
Seq.of(1, 2, 3).sumInt(i -> i);
Seq.of(1, 2, 3).count();
```

## 分块、窗口与分组

```java
Seq<Integer> seq = Seq.of(1, 2, 3, 4, 5, 6, 7, 8, 9);

seq.chunked(3)
    .map(list -> list.join(","))
    .join("|");

// "1,2,3|4,5,6|7,8,9"

seq.windowed(3, 2, true, Reducer::toList)
    .map(Object::toString)
    .join(",");

// "[1, 2, 3],[3, 4, 5],[5, 6, 7],[7, 8, 9],[9]"

seq.groupBy(i -> i % 3);

// {1=[1, 4, 7], 2=[2, 5, 8], 0=[3, 6, 9]}
```

`chunked` 和 `windowed` 使用 `Downstream.Staged`，可以在源流结束后补发最后一个未满批次或未满窗口的结果。

## Downstream 与 Reducer

`Downstream<T, E>` 表示一个可复用的流转换。它把 `Predicate<E>` 转换成 `Predicate<T>`，因此可以同时用于 `Seq.downstream(...)` 和 `Reducer.of(...)`：

```java
Downstream<Integer, String> evenToString = p -> i ->
    (i & 1) == 0 && p.test(Integer.toString(i));

SeqList<String> list = Seq.of(1, 2, 3, 4)
    .downstream(evenToString)
    .toList();
```

实际使用时通常直接调用 `Seq` 上封装好的方法：

```java
Seq.of(1, 2, 3, 4)
    .filter(i -> (i & 1) == 0)
    .map(Object::toString)
    .toList();
```

`Reducer<T, V>` 是带结果的终端 predicate。它可以收集结果，也可以通过 `done()` 表示已经完成，从而触发上游短路：

```java
Integer firstEven = Seq.of(1, 3, 4, 6)
    .reduce(Reducer.first(i -> (i & 1) == 0));

SeqList<String> values = Seq.of(1, 2, null, 3)
    .reduce(Reducer.of(Downstream.filterNotNull(), Reducer.mapping(Object::toString)));
```

## Seq2 与 Map 风格处理

`Seq2<K, V>` 用于键值对或双参数流：

```java
Seq.of(1, 2, 3, 4, 5)
    .toPairs(false)
    .map((a, b) -> a + ":" + b)
    .join(",");

// "1:2,3:4"
```

`SeqMap<K, V>` 继承 `LinkedHashMap<K, V>` 并实现 `Seq2<K, V>`，支持 `mapKeys`、`mapValues`、`swap`、`toKeys`、`toValues`、`groupBy` 等操作。

## ItrSeq、IntSeq 与集合类型

- `ItrSeq<T>` 同时实现 `Iterable<T>` 和 `Seq<T>`，适合需要 iterator 语义的场景，并提供 `gen`、`repeat`、`untilNull` 等生成方法。
- `IntSeq` 是面向 primitive `int` 的流接口，提供 `range`、`map`、`filter`、`boxed`、`sum` 等操作。
- `SeqList`、`SeqSet`、`SeqMap` 是带流式能力的集合类型，可作为普通集合使用，也可继续调用 seq API。

示例：

```java
ItrSeq.gen(1, 1, Integer::sum)
    .take(10)
    .join(",");

// "1,1,2,3,5,8,13,21,34,55"

IntSeq.range(1, 5)
    .boxed()
    .join(",");

// "1,2,3,4"
```

## IO 与文本处理

`ByteSource` 封装字节输入来源，支持文件、URL、资源、byte array 和字符串序列：

```java
ByteSource.of(path)
    .toLines()
    .filter(line -> !line.isEmpty())
    .take(10)
    .toList();
```

`Splitter` 提供轻量字符串切分，并返回 `Seq<String>`：

```java
Splitter.of('#')
    .split("a#b#c")
    .join(",");

// "a,b,c"
```

## 异步流

`Async` 可以把 `Seq` 包装成 `AsyncSeq`，在指定 executor 中消费：

```java
AsyncSeq<Integer> seq = Async.common()
    .toAsync(Seq.of(1, 2, 3, 4));

seq.consume(System.out::println);
seq.joinTask();
```

`AsyncSeq` 只能消费一次；如需重复使用，请先对源流 `cache()`。

## 设计要点

- `Seq.any(Predicate<T>)` 是最小核心，返回值用于短路。
- `Seq` 是推送式流，适合直接表达生成器、树遍历和回调式数据源。
- `ItrSeq` 是拉取式 iterable 适配，适合需要 `Iterator` 的场景。
- `Downstream` 负责流转换复用，`Reducer` 负责终端归约和短路结果。
- 默认操作尽量惰性执行，`toList`、`join`、`reduce` 等终端操作才触发遍历。

## 反馈

可使用中文直接提交 issue，也可添加微信 `radiumlei2010` 进群沟通。

## 发布记录

### 2.2.1

修复 `Seq.toStaged(...)` 的收尾行为。此前 staged downstream 在上游提前短路时可能跳过 `after()`，导致 `take(...).chunked(...)` 这类组合无法补发最后一个未满批次；本版本确保无论上游是否短路，都会执行 staged 收尾逻辑。

当前源码版本。Maven 坐标为 `io.github.wolray:seq:2.2.1`。

### 2.2.0

新增 `mapNotNull`，用于过滤掉映射结果为 `null` 的元素。

### 2.1.0 (2026-02-12)

重新加入 `Seq2` 和 `SeqMap`，并提供 `mapKeys`、`mapValues`、`pairBy`、`pairWith` 等转换方法。

集合类型的 `Seq` 对齐到 `SizedSeq` 抽象。

### 2.0.0 (2026-02-07)

`Seq` 由 consumer 机制切换为 predicate 机制。每次触发回调时都会判断当前流是否需要终止，类似 Go 1.23 的 `range` from `func`，但返回的 boolean 表示终止。

重构 `Reducer` 机制，并去掉独立的 `Transducer` 概念。

### 1.x

早期版本逐步引入了 `IntSeq`、`Lazy`、`SeqList`、`SeqMap`、`SeqSet`、`ByteSource`、`Splitter`、`groupBy`、`toMap`、`windowed` 等能力。

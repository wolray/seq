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
    <version>1.0.0</version>
</dependency>
```

## 完备的流式API
### 流的创建
#### 单位流
#### from Iterable
#### from varargs
#### from regex match
#### from split
#### from tree/DFS

由于内容过多，仍有许多功能尚未实现，暂列于后文。

## todo

#### 更好的groupby

### BatchedSeq与流的缓存
当需要对流进行缓存或者collect时，传统做法是使用`ArrayList`或者`LinkedList`。然而前者需要不时拷贝内部数组，后者每个元素内存成本高，更好的方式是将二者结合，使用扩容后的新数组存放新元素，但不进行数组拷贝，而是直接将新旧数组直接link起来。从而既缓存了元素，也实现了内存和性能的双优化。

### 二元流/多元流
基于callback机制的二元流，是其独特的衍生特性。二元流最大的优势是不产生任何tuple或pair之类的中间数据结构，即用即走，十分优雅。
Java里的`List`/`Set`/`Iterable`都可以在一元流的基础上实现升级改造，二元流则自然对应`Map`，可以衍生许多有趣玩法。

### Splitter
Java的`String`提供了默认的`split`实现，但它会将substring收集为一个数组，在关心性能或后续应用的场景下殊为不妥。Google的guava库提供了产出`Iterable`的`Splitter`，十分优秀。

而基于callback机制，我们能实现出更好更快的`Splitter`，相对guava不仅能有20%-40%的性能提升，还可支持任意后续的流式操作。

### 统一InputStream与数据源
`InputStream`是Java里进行IO交互时一个非常底层且重要的接口，而对于许多常用文件格式，例如CSV/properties，天然一行代表一个数据块。所以`InputStream`和Seq of String是直接对应的。

在另一方面，`InputStream`是一次性的，本身不可重用，从函数式编程的角度来讲，它不算是一种好的数据。所以本项目会对其进行封装，抽象出一个中间数据结构`ISSouce`(意为InputStream Source)。它将对`File`, `Path`, local file, resouce, `URL`以及literal string等常见数据源进行统一收口，实现可重用且IO隔离的安全流式数据源，并提供与`Seq`的互转。

#### 异步流/并发流/异步通道

#### 热流与订阅

## 反馈
可使用中文直接提issue，也可添加微信radiumlei2010进群，原作者也在群里，方便他与大家沟通，收集反馈。

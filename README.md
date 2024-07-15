# seq

本项目提供一个强大而完备的流式编程API，并独创性的为Java添加类似生成器的编程机制。

网页端参考：[阿里云社区](https://developer.aliyun.com/article/1191351?spm=5176.28261954.J_7341193060.5.44812fdeTRXvK5&scm=20140722.S_community@@%E6%96%87%E7%AB%A0@@1191351._.ID_1191351-RL_%E4%B8%80%E7%A7%8D%E6%96%B0%E7%9A%84%E6%B5%81%E4%B8%BA%20java%20%E5%8A%A0%E5%85%A5%E7%94%9F%E6%88%90%E5%99%A8generator%E7%89%B9%E6%80%A7-LOC_m~UND~search~UND~community~UND~i-OR_ser-V_3-P0_1)

手机端参考：[阿里开发者微信公众号](https://mp.weixin.qq.com/s/v-HMKBWxtz1iakxFL09PDw)

这一机制的设计思想与核心代码在上述公开文章中都已阐述完整，本项目是在此基础上的完全重写，未来发展也将主要面向开源社区需求。

文档请参考：[Wiki](https://github.com/wolray/seq/wiki)

## 引用方式

项目已发布到Central Maven，直接引用即可

```xml
<dependency>
    <groupId>io.github.wolray</groupId>
    <artifactId>seq</artifactId>
    <version>1.0.2</version>
</dependency>
```

## 反馈

可使用中文直接提issue，也可添加微信radiumlei2010进群，方便大家沟通，收集反馈。

## 发布记录

#### 1.0.2 (20231110)

新增`IOChain`, `ByteSource`, `Seq2`, `Seq3`, `SeqList`, `SeqMap`, `SeqQueue`, `Splitter`, `BackedSeq`

调整了`Reducer`, `Transducer`的部分逻辑，新增了`groupBy`, `toMap`等功能

#### 1.0.1 (20230922)

新增`IntSeq`, `Lazy`, `BatchedSeq`, `SeqSet`, `windowed`, `timeWindowed`

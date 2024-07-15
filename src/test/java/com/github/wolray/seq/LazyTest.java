package com.github.wolray.seq;

import java.util.function.BinaryOperator;

/**
 * @author wolray
 */
public class LazyTest {
    public static class Context {
        // 定义5个初始节点
        Lazy<String> a = Lazy.of(() -> log(1, "a"));
        Lazy<String> b = Lazy.of(() -> log(1, "b"));
        Lazy<String> c = Lazy.of(() -> log(1, "c"));
        Lazy<String> d = Lazy.of(() -> log(1, "d"));
        Lazy<String> e = Lazy.of(() -> log(1, "e"));

        // 定义加法
        BinaryOperator<String> add = (x, y) -> log(1, String.format("(%s + %s)", x, y));
        // 定义乘法
        BinaryOperator<String> mul = (x, y) -> log(2, String.format("%s * %s", x, y));

        // (a + b)
        Lazy<String> ab = Lazy.of(a, b, add);
        // (d + e)
        Lazy<String> de = Lazy.of(d, e, add);
        // (a + b) * c
        Lazy<String> abc = Lazy.of(ab, c, mul);
        // (d + e) * c
        Lazy<String> dec = Lazy.of(de, c, mul);
        // (a + b) * c + (d + e) * c
        Lazy<String> result = Lazy.of(abc, dec, add);

        private Lazy<Long> start = Lazy.of(System::currentTimeMillis);

        String log(long seconds, String value) {
            Async.delay(seconds * 1000);
            System.out.printf("[%s] %s at %d\n", Thread.currentThread().getName(), value, System.currentTimeMillis() - start.get());
            return value;
        }
    }

    public static void main(String[] args) {
        Context ctx = new Context();
        // 开始计时
        ctx.start.get();
        // 递归计算result节点
        ctx.result.forkJoin();
    }
}

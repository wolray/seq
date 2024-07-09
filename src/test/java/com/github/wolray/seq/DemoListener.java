package com.github.wolray.seq;

import org.junit.Test;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author wolray
 */
public class DemoListener {
    final HotChannel<String> channel;

    public DemoListener() {
        channel = new HotChannel<>();
    }

    /**
     * 待实现的监听方法
     * 必须在方法中添加这两行代码，向管道中提供数据和信号
     */
    public void messageArrived(String msg) {
        channel.offer(msg);
        channel.easyNotify();
    }

    @Test
    public void testAsync() {
        // 1个数据源线程 1个通道线程 2个消费线程
        ExecutorService threadPool = Executors.newFixedThreadPool(8);
        // 基于线程池构建异步处理器
        Async async = Async.of(threadPool);
//        Async async = Async.common();
        DemoListener listener = new DemoListener();
        Random rand = new Random();
        // 每隔100~1000毫秒随机时间，产生一个自然数，模拟消息抵达
        async.submit(() -> {
            long i = 0;
            while (true) {
                listener.messageArrived(Long.toString(i++));
                Async.delay((rand.nextInt(10) + 1) * 100);
            }
        });
        // 产生一个热流，从listener获取数据
        // 由于reducer很快，理论上不需要buffer，保险起见设为10
        Seq<String> seq = listener.channel.shareIn(10, false, async);
        // 每隔4秒收集一次
//        seq.windowedByTime(TimeUnit.SECONDS.toMillis(4))
//            .consume(ls -> System.out.printf("[%s] collector4: %s\n", Thread.currentThread().getName(), ls));
        // 每隔4秒收集一次，间隔1秒
        seq.windowedByTime(TimeUnit.SECONDS.toMillis(4), TimeUnit.SECONDS.toMillis(1))
            .consume(ls -> System.out.printf("[%s] collector4step1: %s\n", Thread.currentThread().getName(), ls));
        // 每隔10秒join一次
        seq.windowedByTime(TimeUnit.SECONDS.toMillis(10), Reducer.join(",", String::toString))
            .consume(s -> System.out.printf("[%s] joiner10: %s\n", Thread.currentThread().getName(), s));
        seq.take(20).println();
    }

    @Test
    public void testSimple() {
        Random rand = new Random();
        Seq<String> seq = c -> {
            new Thread(() -> {
                long i = 0;
                while (true) {
                    c.accept(Long.toString(i++));
                    Async.delay((rand.nextInt(10) + 1) * 100);
                }
            }).start();
        };
        seq.windowedByTime(TimeUnit.SECONDS.toMillis(4), TimeUnit.SECONDS.toMillis(1))
            .consume(ls -> System.out.printf("[%s] collector4step1: %s\n", Thread.currentThread().getName(), ls));
        seq.windowedByTime(TimeUnit.SECONDS.toMillis(4))
            .consume(ls -> System.out.printf("[%s] collector4: %s\n", Thread.currentThread().getName(), ls));
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(100));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

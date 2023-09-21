package com.github.wolray.seq;

import org.junit.Test;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author wolray
 */
public class DemoListener {
    /**
     * 待实现的监听方法
     * 必须在方法中添加这两行代码，向管道中提供数据和信号
     */
    public void messageArrived(String msg) {
    }

    @Test
    public void testSimple() {
        Random rand = new Random();
        Seq<String> seq = c -> {
            new Thread(() -> {
                long i = 0;
                while (true) {
                    c.accept(Long.toString(i++));
                    try {
                        Thread.sleep((rand.nextInt(10) + 1) * 100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
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

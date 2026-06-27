package com.github.wolray.seq;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 复现 BUG_REPORT 中在 2.2.1 上仍存在的两个问题：
 * 1) windowedByTime 末尾时间窗丢失
 * 2) union(t)（原 append）在下游已停止后仍多追加一次
 * 这两个测试若“通过”，说明 bug 已修复；若“失败”，说明 bug 仍存在。
 */
public class BugReportTest {

    /** windowedByTime：最后一个时间窗未被输出，导致元素丢失。 */
    @Test
    public void windowedByTime_dropsLastWindow() {
        // 慢源：每 60ms 产一个，共 5 个；时间窗 100ms
        Seq<Integer> slow = p -> {
            for (int i = 1; i <= 5; i++) {
                try { Thread.sleep(60); } catch (InterruptedException e) {}
                if (p.test(i)) return true;
            }
            return false;
        };

        List<List<Integer>> windows = new ArrayList<>();
        slow.windowedByTime(100, Reducer::toList)
            .consume(w -> windows.add(new ArrayList<>(w)));

        int total = windows.stream().mapToInt(List::size).sum();
        System.out.println("[windowedByTime] 窗口=" + windows + " 累计元素=" + total + " (源共5个)");

        // 期望：5 个元素全部出现在某个窗口里
        Assert.assertEquals("最后时间窗丢失，累计元素应为 5", 5, total);
    }

    /** union(t)：下游在第 2 个元素就喊停，追加的 99 仍被喂进下游。 */
    @Test
    public void unionAppend_appendsAfterDownstreamStop() {
        // Seq.of 版本
        List<Integer> received = new ArrayList<>();
        Seq.of(1, 2, 3).union(99)
            .any(t -> { received.add(t); return received.size() >= 2; });
        System.out.println("[union] Seq.of received=" + received);
        Assert.assertEquals("下游已在第2个喊停，不应再追加99",
            Arrays.asList(1, 2), received);
    }

    /** 纯惰性源（直接实现 any）上的同一问题，排除 ItrSeq 重写干扰。 */
    @Test
    public void unionAppend_appendsAfterDownstreamStop_lazy() {
        List<Integer> received = new ArrayList<>();
        Seq<Integer> base = p -> {
            for (int i = 1; i <= 3; i++) {
                if (p.test(i)) return true;
            }
            return false;
        };
        base.union(99)
            .any(t -> { received.add(t); return received.size() >= 2; });
        System.out.println("[union] 纯惰性源 received=" + received);
        Assert.assertEquals("下游已在第2个喊停，不应再追加99",
            Arrays.asList(1, 2), received);
    }
}


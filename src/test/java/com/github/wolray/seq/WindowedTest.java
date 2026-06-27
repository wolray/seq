package com.github.wolray.seq;

import org.junit.Assert;
import org.junit.Test;

import java.util.*;

/**
 * 参考 BugReportTest 的风格，直接验证 Downstream.windowed 的多种窗口场景。
 */
public class WindowedTest {

    @Test
    public void windowed_overlappingWindows_withPartialTail() {
        List<List<Integer>> windows = collectWindowed(
            Seq.of(1, 2, 3, 4, 5),
            3, 1, true
        );
        System.out.println("[windowed] overlapping partial windows=" + windows);

        Assert.assertEquals("step=1 且 allowPartial=true 时，应输出所有重叠窗口和尾部 partial 窗口",
            Arrays.asList(
                Arrays.asList(1, 2, 3),
                Arrays.asList(2, 3, 4),
                Arrays.asList(3, 4, 5),
                Arrays.asList(4, 5),
                Arrays.asList(5)
            ),
            windows);
    }

    @Test
    public void windowed_overlappingWindows_withoutPartialTail() {
        List<List<Integer>> windows = collectWindowed(
            Seq.of(1, 2, 3, 4, 5),
            3, 1, false
        );
        System.out.println("[windowed] overlapping full windows=" + windows);

        Assert.assertEquals("step=1 且 allowPartial=false 时，只应输出完整重叠窗口",
            Arrays.asList(
                Arrays.asList(1, 2, 3),
                Arrays.asList(2, 3, 4),
                Arrays.asList(3, 4, 5)
            ),
            windows);
    }

    @Test
    public void windowed_nonOverlappingWindows_behavesLikeChunks() {
        List<List<Integer>> windows = collectWindowed(
            Seq.of(1, 2, 3, 4, 5, 6, 7),
            3, 3, true
        );
        System.out.println("[windowed] non-overlapping windows=" + windows);

        Assert.assertEquals("size=step 时，应等价于分块窗口并保留尾部 partial",
            Arrays.asList(
                Arrays.asList(1, 2, 3),
                Arrays.asList(4, 5, 6),
                Arrays.asList(7)
            ),
            windows);
    }

    @Test
    public void windowed_gapWindows_skipsElementsBetweenWindows() {
        List<List<Integer>> windows = collectWindowed(
            Seq.of(1, 2, 3, 4, 5, 6, 7, 8, 9),
            3, 4, true
        );
        System.out.println("[windowed] gap windows=" + windows);

        Assert.assertEquals("step > size 时，窗口之间应跳过元素，并在 allowPartial=true 时保留最后 partial",
            Arrays.asList(
                Arrays.asList(1, 2, 3),
                Arrays.asList(5, 6, 7),
                Arrays.asList(9)
            ),
            windows);
    }

    @Test
    public void windowed_sourceShorterThanSize_respectsAllowPartial() {
        List<List<Integer>> partialAllowed = collectWindowed(
            Seq.of(1, 2),
            3, 1, true
        );
        List<List<Integer>> partialDisallowed = collectWindowed(
            Seq.of(1, 2),
            3, 1, false
        );
        System.out.println("[windowed] short source allowPartial=true windows=" + partialAllowed);
        System.out.println("[windowed] short source allowPartial=false windows=" + partialDisallowed);

        Assert.assertEquals("源长度小于 size 且 allowPartial=true 时，应输出已有元素组成的 partial 窗口",
            Arrays.asList(Arrays.asList(1, 2), Arrays.asList(2)),
            partialAllowed);
        Assert.assertEquals("源长度小于 size 且 allowPartial=false 时，不应输出窗口",
            Arrays.asList(),
            partialDisallowed);
    }

    @Test
    public void windowed_emptySource_emitsNoWindows() {
        List<List<Integer>> windows = collectWindowed(Seq.empty(), 3, 1, true);
        System.out.println("[windowed] empty source windows=" + windows);

        Assert.assertEquals("空源不应输出任何窗口", Arrays.asList(), windows);
    }

    @Test
    public void windowed_stopsWhenDownstreamStops() {
        List<List<Integer>> received = new ArrayList<>();

        Seq.of(1, 2, 3, 4, 5)
            .toStaged(Downstream.windowed(3, 1, true, Reducer::toList))
            .any(w -> {
                received.add(new ArrayList<>(w));
                return true;
            });
        System.out.println("[windowed] downstream stopped received=" + received);

        Assert.assertEquals("下游在第一个窗口停止后，不应继续通过 after() 输出尾部窗口",
            Arrays.asList(Arrays.asList(1, 2, 3)),
            received);
    }

    @Test
    public void windowed_rejectsNonPositiveSizeOrStep() {
        assertIllegalArgument("size <= 0 应拒绝", () -> Downstream.windowed(0, 1, true, Reducer::toList));
        assertIllegalArgument("step <= 0 应拒绝", () -> Downstream.windowed(1, 0, true, Reducer::toList));
    }

    private static List<List<Integer>> collectWindowed(
        Seq<Integer> seq,
        int size,
        int step,
        boolean allowPartial
    ) {
        List<List<Integer>> windows = new ArrayList<>();
        seq.toStaged(Downstream.windowed(size, step, allowPartial, Reducer::toList))
            .consume(w -> windows.add(new ArrayList<>(w)));
        return windows;
    }

    private static void assertIllegalArgument(String message, Runnable runnable) {
        try {
            runnable.run();
            Assert.fail(message);
        } catch (IllegalArgumentException expected) {
            System.out.println("[windowed] expected exception=" + expected.getMessage());
        }
    }
}

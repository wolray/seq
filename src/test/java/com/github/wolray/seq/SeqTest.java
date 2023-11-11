package com.github.wolray.seq;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * @author wolray
 */
public class SeqTest {
    @Test
    public void testResult() {
        Seq<Integer> seq1 = Seq.of(0, 2, 4, 1, 6, 3, 8, 10, 11, 12);
        Seq<Integer> filtered1 = seq1.take(5);
        assertTo(filtered1, "0,2,4,1,6");
        assertTo(filtered1, "0,2,4,1,6");
        assertTo(filtered1.reverse(), "6,1,4,2,0");

        IntSeq seq2 = IntSeq.of(0, 2, 4, 1, 6, 3, 8, 10, 11, 12);
        IntSeq filtered2 = seq2.take(5);
        assertTo(filtered2.boxed(), "0,2,4,1,6");
        assertTo(filtered2.boxed(), "0,2,4,1,6");

        Predicate<Integer> predicate = i -> (i & 1) == 0;
        assertTo(seq1.dropWhile(predicate), "1,6,3,8,10,11,12");
        assertTo(seq2.dropWhile(i -> (i & 1) == 0).boxed(), "1,6,3,8,10,11,12");
        assertTo(seq1.takeWhile(predicate), "0,2,4");
        assertTo(seq1.take(5), "0,2,4,1,6");
        assertTo(seq1.take(5).drop(2), "4,1,6");

        Seq<Integer> token1 = Seq.gen(() -> 1).take(5);
        assertTo(token1, "1,1,1,1,1");
        assertTo(token1, "1,1,1,1,1");
        IntSeq token2 = IntSeq.gen(() -> 1).take(5);
        assertTo(token2.boxed(), "1,1,1,1,1");
        assertTo(token2.boxed(), "1,1,1,1,1");

        assertTo(Seq.repeat(5, 1), "1,1,1,1,1");
        assertTo(IntSeq.repeat(5, 1).boxed(), "1,1,1,1,1");
        assertTo(IntSeq.range(0, 10, 2).boxed(), "0,2,4,6,8");

        assertTo(Seq.of(1, 1, 1, 2, 2).distinct(), "1,2");
    }

    @Test
    public void testRunningFold() {
        Seq<Integer> seq = Seq.of(0, 2, 4, 1, 6, 3, 5, 7, 10, 11, 12);
        assertTo(seq.runningFold(0, Integer::sum), "0,2,6,7,13,16,21,28,38,49,61");

        IntSeq is = IntSeq.of(0, 2, 4, 1, 6, 3, 5, 7, 10, 11, 12);
        assertTo(is.runningFold(0, Integer::sum).boxed(), "0,2,6,7,13,16,21,28,38,49,61");
    }

    @Test
    public void partitionTest() {
        Seq<Integer> seq = Seq.of(0, 2, 4, 1, 6, 3, 5, 7, 10, 11, 12);
        Pair<BatchedSeq<Integer>, BatchedSeq<Integer>> pair1 = seq.reduce(Reducer.partition(i -> (i & 1) > 0));
        assertTo(pair1.first, "1,3,5,7,11");
        assertTo(pair1.second, "0,2,4,6,10,12");
    }

    @Test
    public void testChunked() {
        List<Integer> list = Arrays.asList(0, 2, 4, 1, 6, 3, 5, 7, 10, 11, 12);
        Function<ArraySeq<Integer>, String> function = s -> s.join(",");
        assertTo(Seq.of(list).chunked(2).map(function), "|", "0,2|4,1|6,3|5,7|10,11|12");
        assertTo(Seq.of(list).chunked(3).map(function), "|", "0,2,4|1,6,3|5,7,10|11,12");
        assertTo(Seq.of(list).chunked(4).map(function), "|", "0,2,4,1|6,3,5,7|10,11,12");
        assertTo(Seq.of(list).chunked(5).map(function), "|", "0,2,4,1,6|3,5,7,10,11|12");
        assertTo(Seq.of(1, 2, 3, 4).chunked(2).map(function), "|", "1,2|3,4");
        assertTo(Seq.empty().chunked(2), "[]");
    }

    @Test
    public void testYield() {
        Seq<Integer> fib1 = Seq.gen(1, 1, Integer::sum).take(10);
        assertTo(fib1, "1,1,2,3,5,8,13,21,34,55");
        assertTo(fib1, "1,1,2,3,5,8,13,21,34,55");
        IntSeq fib2 = IntSeq.gen(1, 1, Integer::sum).take(10);
        assertTo(fib2.boxed(), "1,1,2,3,5,8,13,21,34,55");
        assertTo(fib2.boxed(), "1,1,2,3,5,8,13,21,34,55");

        Seq<Integer> quad1 = Seq.gen(1, i -> i * 2).take(10);
        assertTo(quad1, "1,2,4,8,16,32,64,128,256,512");
        assertTo(quad1, "1,2,4,8,16,32,64,128,256,512");
        IntSeq quad2 = IntSeq.gen(1, i -> i * 2).take(10);
        assertTo(quad2.boxed(), "1,2,4,8,16,32,64,128,256,512");
        assertTo(quad2.boxed(), "1,2,4,8,16,32,64,128,256,512");

        List<Integer> list1 = Arrays.asList(10, 20, 30);
        List<Integer> list2 = Arrays.asList(1, 2, 3);
        Seq<Integer> cart1 = c -> {
            for (Integer i1 : list1) {
                for (Integer i2 : list2) {
                    c.accept(i1 + i2);
                }
            }
        };
        assertTo(cart1, "11,12,13,21,22,23,31,32,33");
        assertTo(cart1, "11,12,13,21,22,23,31,32,33");
        IntSeq cart2 = c -> {
            for (Integer i1 : list1) {
                for (Integer i2 : list2) {
                    c.accept(i1 + i2);
                }
            }
        };
        assertTo(cart2.boxed(), "11,12,13,21,22,23,31,32,33");
        assertTo(cart2.boxed(), "11,12,13,21,22,23,31,32,33");
    }

    @Test
    public void testArraySeq() {
        ArraySeq<Integer> list = new ArraySeq<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.set(0, 6);
        list.add(2, 10);
        assertTo(list, "6,2,10,3");
    }

    @Test
    public void testSubLists() {
        assertTo(IntSeq.of("233(ab:c)114514(d:e:f:g)42")
                .mapToObj(i -> (char)i)
                .mapSub('(', ')')
                .map(ls -> ls.join(""))
            , "(ab:c),(d:e:f:g)");
    }

    @Test
    public void testWhileEquals() {
        Seq<Integer> seq = Seq.of(1, 1, 2, 3, 4, 6);
        assertTo(seq.takeWhileEquals(), "1,1");
        assertTo(seq.takeWhileEquals(i -> i / 4), "1,1,2,3");
        assertTo(seq.drop(1).takeWhile((i, j) -> i + 1 == j), "1,2,3,4");
    }

    @Test
    public void testToArray() {
        Seq<Integer> seq = Seq.of(1, 1, 2, 3, 4, 6);
        assertTo(Seq.of(seq.toObjArray(Integer[]::new)), "1,1,2,3,4,6");
        assertTo(IntSeq.of(seq.toIntArray(i -> i)).boxed(), "1,1,2,3,4,6");
        assertTo(IntSeq.of(seq.mapToInt(i -> i).toArray()).boxed(), "1,1,2,3,4,6");
    }

    @Test
    public void testReducer() {
        Seq<Integer> seq = Seq.of(1, 2, null, 3, null, 4);
        assertTo(seq.reduce(Reducer.filtering(Objects::nonNull, Reducer.mapping(Object::toString)))
            , "1,2,3,4");
    }

    @Test
    public void testParallel() {
        ForkJoinPool pool = ForkJoinPool.commonPool();
        IntSeq.range(10).boxed().map(t -> pool.submit(() -> {
            System.out.printf("[%s]: %d\n", Thread.currentThread().getName(), t);
            try {
                Thread.sleep(1);
            } catch (InterruptedException e1) {
                throw new RuntimeException(e1);
            }
        })).cache().consume(ForkJoinTask::join);
    }

    @Test
    public void testDuplicate() {
        Seq<Integer> seq = Seq.of(1, 2, 3, 4);
        assertTo(seq.duplicateIf(2, i -> i % 2 > 0), "1,1,2,3,3,4");
        assertTo(seq.duplicateEach(2), "1,1,2,2,3,3,4,4");
        assertTo(seq.duplicateAll(2), "1,2,3,4,1,2,3,4");
        assertTo(seq.circle().take(7), "1,2,3,4,1,2,3");

        IntSeq is = IntSeq.of(1, 2, 3, 4);
        assertTo(is.duplicateIf(2, i -> i % 2 > 0).boxed(), "1,1,2,3,3,4");
        assertTo(is.duplicateEach(2).boxed(), "1,1,2,2,3,3,4,4");
        assertTo(is.duplicateAll(2).boxed(), "1,2,3,4,1,2,3,4");
        assertTo(is.circle().take(7).boxed(), "1,2,3,4,1,2,3");
    }

    @Test
    public void testMatch() {
        String a = "(ab)cd(efg)(h)ijk(lmn)op(q";
        assertTo(Seq.match(a, Pattern.compile("\\((\\w+)\\)")).map(m -> m.group(1)), "ab,efg,h,lmn");
    }

    @Test
    public void testWindowed() {
        Seq<Integer> seq = Seq.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
        assertTo(seq.chunked(3).toList(), "[1, 2, 3],[4, 5, 6],[7, 8, 9]");
        assertTo(seq.chunked(4).toList(), "[1, 2, 3, 4],[5, 6, 7, 8],[9]");
        assertTo(seq.windowed(3, 1, true).toList(), "[1, 2, 3],[2, 3, 4],[3, 4, 5],[4, 5, 6],[5, 6, 7],[6, 7, 8],[7, 8, 9],[8, 9],[9]");
        assertTo(seq.windowed(3, 1, false).toList(), "[1, 2, 3],[2, 3, 4],[3, 4, 5],[4, 5, 6],[5, 6, 7],[6, 7, 8],[7, 8, 9]");
        assertTo(seq.windowed(3, 2, true).toList(), "[1, 2, 3],[3, 4, 5],[5, 6, 7],[7, 8, 9],[9]");
        assertTo(seq.windowed(3, 2, false).toList(), "[1, 2, 3],[3, 4, 5],[5, 6, 7],[7, 8, 9]");
        assertTo(seq.windowed(3, 4, true).toList(), "[1, 2, 3],[5, 6, 7],[9]");
        assertTo(seq.windowed(3, 4, false).toList(), "[1, 2, 3],[5, 6, 7]");
    }

    @Test
    public void testTree() {
        Node n0 = new Node(0);
        Node n1 = new Node(1);
        Node n2 = new Node(2);
        Node n3 = new Node(3);
        Node n4 = new Node(4);
        Node n5 = new Node(5);
        n0.left = n1;
        n0.right = n2;
        n1.left = n3;
        n1.right = n4;
        n2.left = n5;
        Seq<Node> seq = Seq.ofTree(n0, n -> Seq.of(n.left, n.right));
        assertTo(seq.map(n -> n.value), "0,1,3,4,2,5");
//        Seq.ofTreeParallel(n0, 2, n -> Seq.of(n.left, n.right)).map(n -> n.value).printAll(",");
    }

    public static void assertTo(Seq<?> seq, String s) {
        assertTo(seq, ",", s);
    }

    public static void assertTo(Seq<?> seq, String sep, String s) {
        String result = seq.join(sep);
        assert result.equals(s) : result;
    }

    static class Node {
        final int value;
        Node left;
        Node right;

        Node(int value) {
            this.value = value;
        }
    }
}

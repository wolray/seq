package com.github.wolray.seq;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
        ItrSeq<Integer> seq2 = ItrSeq.of(0, 2, 4, 1, 6, 3, 8, 10, 11, 12);
        Seq<Integer> filtered1 = seq1.take(5);
        ItrSeq<Integer> filtered2 = seq2.take(5);

        assertTo(filtered1, "0,2,4,1,6");
        assertTo(filtered2, "0,2,4,1,6");
        assertTo(filtered1.replace(2, i -> i + 100), "100,102,4,1,6");

        assertTo(filtered1.reverse(), "6,1,4,2,0");
        assertTo(filtered2.reverse(), "6,1,4,2,0");

        Predicate<Integer> predicate = i -> (i & 1) == 0;
        assertTo(seq1.dropWhile(predicate), "1,6,3,8,10,11,12");
        assertTo(seq2.dropWhile(predicate), "1,6,3,8,10,11,12");

        assertTo(seq1.dropWhile(i -> (i & 1) == 0), "1,6,3,8,10,11,12");
        assertTo(seq2.dropWhile(i -> (i & 1) == 0), "1,6,3,8,10,11,12");

        assertTo(seq1.takeWhile(predicate), "0,2,4");
        assertTo(seq2.takeWhile(predicate), "0,2,4");

        assertTo(seq1.take(5), "0,2,4,1,6");
        assertTo(seq2.take(5), "0,2,4,1,6");

        assertTo(seq1.take(5).drop(2), "4,1,6");
        assertTo(seq2.take(5).drop(2), "4,1,6");

        Seq<Integer> token1 = ItrSeq.gen(() -> 1).take(5);
        assertTo(token1, "1,1,1,1,1");

        assertTo(ItrSeq.repeat(5, 1), "1,1,1,1,1");
        assertTo(ItrSeq.of(1, 1, 1, 2, 2).distinct(), "1,2");

        assertTo(ItrSeq.range(4), "0,1,2,3");
        assertTo(ItrSeq.range(1, 4), "1,2,3");
    }

    @Test
    public void testRunningFold() {
        ItrSeq<Integer> seq1 = ItrSeq.of(2, 4, 1, 6, 3, 5, 7, 10, 11, 12);
        Seq<Integer> seq2 = Seq.of(2, 4, 1, 6, 3, 5, 7, 10, 11, 12);
        assertTo(seq1.runningFold(1, Integer::sum), "3,7,8,14,17,22,29,39,50,62");
        assertTo(seq2.runningFold(1, Integer::sum), "3,7,8,14,17,22,29,39,50,62");
        assertTo(seq1.runningFold(1, Integer::sum).take(4), "3,7,8,14");
        assertTo(seq2.runningFold(1, Integer::sum).take(4), "3,7,8,14");
    }

    @Test
    public void partitionTest() {
        Seq<Integer> seq = Seq.of(0, 2, 4, 1, 6, 3, 5, 7, 10, 11, 12);
        Pair<SeqList<Integer>, SeqList<Integer>> pair1 = seq.reduce(Reducer.partition(i -> (i & 1) > 0));
        assertTo(pair1.first, "1,3,5,7,11");
        assertTo(pair1.second, "0,2,4,6,10,12");
        Pair<Integer, Integer> pair2 = seq.reduce(Reducer.partition(i -> (i & 1) > 0, Reducer.first()));
        assert pair2.first.equals(1);
        assert pair2.second.equals(0);
        Pair<Integer, Integer> pair3 = seq.reduce(Reducer.partition(i -> (i & 1) > 0, Reducer.first(i -> i > 4)));
        assert pair3.first.equals(5);
        assert pair3.second.equals(6);
        Pair<SeqList<Integer>, SeqList<Integer>> pair4 = seq.reduce(Reducer.partition(i -> (i & 1) > 0, Reducer.of(Downstream.take(4))));
        assertTo(pair4.first, "1,3,5,7");
        assertTo(pair4.second, "0,2,4,6");
    }

    @Test
    public void testChunked() {
        Seq<Integer> seq = Seq.of(0, 2, 4, 1, 6, 3, 5, 7, 10, 11, 12);
        Function<SeqList<Integer>, String> function = s -> s.join(",");
        assertTo(seq.chunked(2).map(function), "|", "0,2|4,1|6,3|5,7|10,11|12");
        assertTo(seq.chunked(3).map(function), "|", "0,2,4|1,6,3|5,7,10|11,12");
        assertTo(seq.chunked(4).map(function), "|", "0,2,4,1|6,3,5,7|10,11,12");
        assertTo(seq.chunked(5).map(function), "|", "0,2,4,1,6|3,5,7,10,11|12");
        assertTo(seq.windowed(5, 3, true, Reducer.toList()).map(function), "|", "0,2,4,1,6|1,6,3,5,7|5,7,10,11,12|11,12");
        assertTo(seq.windowed(5, 3, false, Reducer.toList()).map(function), "|", "0,2,4,1,6|1,6,3,5,7|5,7,10,11,12");
        assertTo(Seq.of(1, 2, 3, 4).chunked(2).map(function), "|", "1,2|3,4");
        Seq<Integer> empty = Seq.empty();
        Seq<SeqList<Integer>> chunked = empty.chunked(2);
        assertTo(chunked, "[]");
        assertTo(seq.chunked(4, Reducer.of(Downstream.take(2))).map(function), "|", "0,2|6,3|10,11");
    }

    @Test
    public void testGroupBy() {
        Seq<Integer> seq = Seq.of(0, 2, 4, 1, 6, 3, 5, 7, 10, 11, 12);
        assert seq.groupBy(i -> i / 4).toString().equals("{0=[0, 2, 1, 3], 1=[4, 6, 5, 7], 2=[10, 11], 3=[12]}");
        assert seq.groupBy(i -> i / 4, Reducer.first(i -> i % 4 == 0)).toString().equals("{0=0, 1=4, 2=null, 3=12}");
        assert seq.groupBy(i -> i / 4, Reducer.sumInt()).toString().equals("{0=6, 1=22, 2=21, 3=12}");
        assert seq.groupBy(i -> i / 4, Reducer.first()).toString().equals("{0=0, 1=4, 2=10, 3=12}");
        assert seq.groupBy(i -> i / 4, Reducer.of(Downstream.chunked(2))).toString().equals("{0=[[0, 2], [1, 3]], 1=[[4, 6], [5, 7]], 2=[[10, 11]], 3=[[12]]}");
    }

    @Test
    public void testYield() {
        ItrSeq<Integer> fib1 = ItrSeq.gen(1, 1, Integer::sum).take(10);
        assertTo(fib1, "1,1,2,3,5,8,13,21,34,55");

        ItrSeq<Integer> quad1 = ItrSeq.gen(1, i -> i * 2).take(10);
        assertTo(quad1, "1,2,4,8,16,32,64,128,256,512");

        List<Integer> list1 = Arrays.asList(10, 20, 30);
        List<Integer> list2 = Arrays.asList(1, 2, 3);
        Seq<Integer> cart1 = p -> {
            for (Integer i1 : list1) {
                for (Integer i2 : list2) {
                    if (p.test(i1 + i2)) {
                        return true;
                    }
                }
            }
            return false;
        };
        assertTo(cart1, "11,12,13,21,22,23,31,32,33");
        assertTo(cart1.take(4), "11,12,13,21");
        assertTo(cart1.drop(4), "22,23,31,32,33");
    }

    @Test
    public void testSeqList() {
        SeqList<Integer> list = new SeqList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.set(0, 6);
        list.add(2, 10);
        assertTo(list, "6,2,10,3");
    }

    @Test
    public void testWhileEquals() {
        Seq<Integer> seq1 = Seq.of(1, 1, 1, 2, 3, 4, 6);
        ItrSeq<Integer> seq2 = ItrSeq.of(1, 1, 1, 2, 3, 4, 6);
        assertTo(seq1.takeWhileEquals(), "1,1,1");
        assertTo(seq2.takeWhileEquals(), "1,1,1");
    }

    @Test
    public void testToArray() {
        Seq<Integer> seq1 = Seq.of(1, 1, 2, 3, 4, 6);
        ItrSeq<Integer> seq2 = ItrSeq.of(1, 1, 2, 3, 4, 6);
        assertTo(Seq.of(seq1.toObjArray(Integer[]::new)), "1,1,2,3,4,6");
        assertTo(Seq.of(seq2.toObjArray(Integer[]::new)), "1,1,2,3,4,6");
    }

    @Test
    public void testReducer() {
        Seq<Integer> seq = Seq.of(1, 2, null, 3, null, 4);
        assertTo(seq.reduce(Reducer.of(Downstream.filterNotNull(), Reducer.mapping(Object::toString))), "1,2,3,4");
        assertTo(seq.filterNotNull().reduce(Reducer.of(Downstream.map(Object::toString))), "1,2,3,4");
        assertTo(seq.filterNotNull().reduce(Reducer.mapping(Object::toString)), "1,2,3,4");
        assertTo(seq.filterNotNull().map(Object::toString), "1,2,3,4");
    }

    @Test
    public void testDuplicate() {
        Seq<Integer> seq = Seq.of(1, 2, 3, 4);
        assertTo(seq.duplicateIf(2, i -> i % 2 > 0), "1,1,2,3,3,4");
        assertTo(seq.duplicateEach(2), "1,1,2,2,3,3,4,4");
        assertTo(seq.duplicateAll(2), "1,2,3,4,1,2,3,4");
        assertTo(seq.union(5), "1,2,3,4,5");
        assertTo(seq.downstream(Downstream.union(5)), "1,2,3,4,5");
    }

    @Test
    public void testMatch() {
        String a = "(ab)cd(efg)(h)ijk(lmn)op(q";
        assertTo(Seq.match(a, Pattern.compile("\\((\\w+)\\)")).map(m -> m.group(1)), "ab,efg,h,lmn");
    }

    @Test
    public void testWindowed() {
        Seq<Integer> seq = Seq.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
        assertTo(seq.chunked(3), "[1, 2, 3],[4, 5, 6],[7, 8, 9]");
        assertTo(seq.chunked(3).take(2), "[1, 2, 3],[4, 5, 6]");
        assertTo(seq.chunked(3).drop(2), "[7, 8, 9]");
        assertTo(seq.chunked(4), "[1, 2, 3, 4],[5, 6, 7, 8],[9]");
        assertTo(seq.windowed(3, 1, true, Reducer.toList()), "[1, 2, 3],[2, 3, 4],[3, 4, 5],[4, 5, 6],[5, 6, 7],[6, 7, 8],[7, 8, 9],[8, 9],[9]");
        assertTo(seq.windowed(3, 1, false, Reducer.toList()), "[1, 2, 3],[2, 3, 4],[3, 4, 5],[4, 5, 6],[5, 6, 7],[6, 7, 8],[7, 8, 9]");
        assertTo(seq.windowed(3, 2, true, Reducer.toList()), "[1, 2, 3],[3, 4, 5],[5, 6, 7],[7, 8, 9],[9]");
        assertTo(seq.windowed(3, 2, false, Reducer.toList()), "[1, 2, 3],[3, 4, 5],[5, 6, 7],[7, 8, 9]");
        assertTo(seq.windowed(3, 4, true, Reducer.toList()), "[1, 2, 3],[5, 6, 7],[9]");
        assertTo(seq.windowed(3, 4, false, Reducer.toList()), "[1, 2, 3],[5, 6, 7]");
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
    }

    @Test
    public void testSplitter() {
        Splitter splitter1 = Splitter.of('#');
        assertTo(splitter1.split("a#b#c#d#e"), "a,b,c,d,e");
        assertTo(splitter1.split("a#b#c#d#"), "a,b,c,d,");

        Splitter splitter2 = Splitter.of("..");
        assertTo(splitter2.split("a..b..c..d..e."), "a,b,c,d,e.");
        assertTo(splitter2.split("a..b..c..d..e"), "a,b,c,d,e");
        assertTo(splitter2.split("a..b..c..d.."), "a,b,c,d,");
        assertTo(splitter2.split("a..b..c..d."), "a,b,c,d.");

        Splitter splitter3 = Splitter.of(Pattern.compile("[abc]"));
        assertTo(splitter3.split("1a2b3c4"), "1,2,3,4");
        assertTo(splitter3.split("1a2b3c"), "1,2,3,");
        assertTo(splitter3.split("1a2b3c").take(2), "1,2");
        assertTo(splitter3.split("1a2b3c").drop(2), "3,");
    }

    @Test
    public void testTreeSeq() {
        SeqMap<Integer, Integer> map = new SeqMap<>();
        map.put(1, 0);
        map.put(2, 0);
        map.put(3, 1);
        map.put(4, 1);
        map.put(6, 1);
        map.put(7, 2);
        map.put(8, 7);
        map.put(9, 7);
        SeqMap<Integer, SeqList<Integer>> tree = map.swap().groupBy();
        TreeSeq<Integer> seq = new TreeSeq<>(0, n -> {
            List<Integer> list = tree.get(n);
            return list != null ? list.iterator() : Collections.emptyIterator();
        });
        System.out.println(seq.print(Object::toString));
    }

    static void assertTo(Seq<?> seq, String s) {
        assertTo(seq, ",", s);
    }

    static void assertTo(Seq<?> seq, String sep, String s) {
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

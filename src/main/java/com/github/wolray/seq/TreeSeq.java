package com.github.wolray.seq;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author wolray
 */
public class TreeSeq<N> implements Seq<N> {
    public final N root;
    private final Function<N, Iterator<N>> toSub;

    public TreeSeq(N root, Function<N, Iterator<N>> toSub) {
        this.root = root;
        this.toSub = toSub;
    }

    @Override
    public void consume(Consumer<N> consumer) {
        SeqExpand<N> expand = SeqExpand.of(n -> toSub.apply(n)::forEachRemaining);
        expand.scan(consumer, root);
    }

    public String print(Function<N, String> toName) {
        Seq<String> seq = c -> print(c, root, toSub.apply(root), toName, "", "");
        return seq.join("\n");
    }

    private void print(Consumer<String> c, N node, Iterator<N> itr,
        Function<N, String> toName, String prefix, String subPrefix) {
        c.accept(prefix + toName.apply(node));
        while (itr.hasNext()) {
            N cur = itr.next();
            Iterator<N> sub = toSub.apply(cur);
            print(c, cur, sub, toName,
                subPrefix + (sub.hasNext() ? "|-- " : "+-- "),
                subPrefix + (itr.hasNext() ? "|   " : "    "));
        }
    }

    public static void main(String[] args) {
        Map<Integer, Integer> map = new HashMap<>();
        map.put(1, 0);
        map.put(2, 0);
        map.put(3, 1);
        map.put(4, 1);
        map.put(6, 1);
        map.put(7, 2);
        map.put(8, 7);
        map.put(9, 8);
        map.put(10, 9);
        Map<Integer, List<Integer>> tree = new HashMap<>();
        map.forEach((n, p) -> tree.computeIfAbsent(p, k -> new ArrayList<>()).add(n));
        TreeSeq<Integer> seq = new TreeSeq<>(0, n -> {
            List<Integer> list = tree.get(n);
            return list != null ? list.iterator() : Collections.emptyIterator();
        });
        System.out.println(seq.print(Object::toString));
    }
}
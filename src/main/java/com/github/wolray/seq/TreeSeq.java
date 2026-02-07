package com.github.wolray.seq;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

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
    public boolean until(Predicate<N> stop) {
        SeqExpand<N> expand = SeqExpand.of(n -> Seq.of(() -> toSub.apply(n)));
        return expand.scan(stop, root);
    }

    public String print(Function<N, String> toName) {
        Seq<String> seq = p -> print(p, root, toSub.apply(root), toName, "", "");
        return seq.join("\n");
    }

    private boolean print(Predicate<String> p, N node, Iterator<N> itr,
        Function<N, String> toName, String prefix, String subPrefix) {
        if (p.test(prefix + toName.apply(node))) {
            return true;
        }
        while (itr.hasNext()) {
            N cur = itr.next();
            Iterator<N> sub = toSub.apply(cur);
            boolean stop = print(p, cur, sub, toName,
                subPrefix + (sub.hasNext() ? "|-- " : "+-- "),
                subPrefix + (itr.hasNext() ? "|   " : "    "));
            if (stop) {
                return true;
            }
        }
        return false;
    }
}
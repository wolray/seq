package com.github.wolray.seq;

import guru.nidi.graphviz.attribute.Font;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Factory;
import guru.nidi.graphviz.model.Graph;
import guru.nidi.graphviz.model.LinkSource;
import guru.nidi.graphviz.model.Node;
import org.junit.Test;

import java.io.File;
import java.util.Map;

/**
 * @author wolray
 */
public class SeqClassesTest {
    public static final SeqExpand<Class<?>> CLASS_EXPAND = cls -> Seq.of(cls.getInterfaces()).append(cls.getSuperclass());

    public static Graph graph(Map<Class<?>, ArraySeq<Class<?>>> map) {
        Map<Class<?>, Pair<Class<?>, Node>> nodeMap = SeqMap.of(map).mapByValue((cls, parents) -> {
            Node nd = Factory.node(cls.getSimpleName());
            if (!cls.isInterface()) {
                nd = nd.with(Shape.BOX);
            }
            return new Pair<>(cls, nd);
        });
        Seq<LinkSource> linkSources = c -> nodeMap.forEach((name, pair) -> {
            Node curr = pair.second;
            for (Class<?> parent : map.get(pair.first)) {
                c.accept(nodeMap.get(parent).second.link(curr));
            }
        });
        return Factory.graph("Classes").directed()
            .graphAttr().with(Rank.dir(Rank.RankDir.LEFT_TO_RIGHT))
            .nodeAttr().with(Font.name("Consolas"))
            .linkAttr().with("class", "link-class")
            .with(linkSources.toObjArray(LinkSource[]::new));
    }

    @Test
    public void testClasses() {
        Seq<Class<?>> ignore = Seq.of(Seq0.class, Object.class);
        Map<Class<?>, ArraySeq<Class<?>>> map = CLASS_EXPAND
            .filterNot(ignore.toSet()::contains)
            .terminate(cls -> cls.getName().startsWith("java"))
            .toDAG(Seq.of(ArraySeq.class, LinkedSeq.class, ConcurrentSeq.class, LinkedSeqSet.class, BatchedSeq.class));
        Graph graph = graph(map);
        IOChain.apply(String.format("src/test/resources/%s.svg", "seq-classes"),
            s -> Graphviz.fromGraph(graph).render(Format.SVG).toFile(new File(s)));
    }

    @Test
    public void testSeqMap() {
        Seq<Class<?>> ignore = Seq.of(Seq0.class);
        Map<Class<?>, ArraySeq<Class<?>>> map = CLASS_EXPAND
            .filterNot(ignore.toSet()::contains)
            .terminate(cls -> cls.getName().startsWith("java"))
            .toDAG(Seq.of(LinkedSeqMap.class));
        Graph graph = graph(map);
        IOChain.apply(String.format("src/test/resources/%s.svg", "seq-map"),
            s -> Graphviz.fromGraph(graph).render(Format.SVG).toFile(new File(s)));
    }
}

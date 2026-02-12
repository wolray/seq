package com.github.wolray.seq;

import org.junit.Test;

import static com.github.wolray.seq.SeqTest.assertTo;

public class Seq2Test {
    @Test
    public void testMapPair() {
        assertTo(Seq.of(1, 2, 3, 4, 5, 6).toPairs(true).map((a, b) -> a + ":" + b), "1:2,2:3,3:4,4:5,5:6");
        assertTo(Seq.of(1, 2, 3, 4, 5, 6).toPairs(false).map((a, b) -> a + ":" + b), "1:2,3:4,5:6");
        assertTo(Seq.of(1, 2, 3, 4, 5).toPairs(false).map((a, b) -> a + ":" + b), "1:2,3:4");
        assertTo(Seq.of(1, 2, 3, 4, 5).toPairs(false).mapIf((p, i, j) -> p.test(i + "@" + j)), "1@2,3@4");
    }

    @Test
    public void testParser() {
        String s = "1:2,2:3,3:4,4:5,5:6";
        char[] chars = s.toCharArray();
        assertTo(Splitter.parsePairs(chars, ',', ':').map((a, b) -> a + ":" + b), s);
    }
}

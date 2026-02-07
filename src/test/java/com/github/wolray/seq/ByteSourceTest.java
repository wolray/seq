package com.github.wolray.seq;

import org.junit.Test;

public class ByteSourceTest {
    @Test
    public void testByteSource() {
        byte[] bytes1 = ByteSource.ofResource(ByteSourceTest.class, "/seq-classes.svg").toBytes();
        String join = ByteSource.ofResource(ByteSourceTest.class, "/seq-classes.svg").toLines().join("\n");
        byte[] bytes2 = (join + '\n').getBytes();
        assert bytes1.length == bytes2.length;
        for (int i = 0; i < bytes1.length; i++) {
            assert bytes1[i] == bytes2[i];
        }
    }
}

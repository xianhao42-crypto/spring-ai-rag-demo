package com.xianhao.rag.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author 13108
 */
class TextChunkerTest {

    @Test
    void splitsByLine() {
        List<String> chunks = TextChunker.chunk("第一行内容\n第二行内容", 300, 50);
        assertEquals(2, chunks.size());
        assertEquals("第一行内容", chunks.get(0));
        assertEquals("第二行内容", chunks.get(1));
    }

    @Test
    void slidingWindowWhenLong() {
        String longLine = "a".repeat(650);
        List<String> chunks = TextChunker.chunk(longLine, 300, 50);
        assertTrue(chunks.size() >= 3);
        assertEquals(300, chunks.get(0).length());
    }
}

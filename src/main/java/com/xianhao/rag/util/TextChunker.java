package com.xianhao.rag.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本切片（与 Day12 相同策略）：
 * 1) 按换行拆段
 * 2) 超长段按滑动窗口继续切
 *
 * @author 13108
 */
public final class TextChunker {

    private TextChunker() {
    }

    public static List<String> chunk(String text, int chunkSize, int overlap) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize 必须 > 0");
        }
        if (overlap < 0 || overlap >= chunkSize) {
            throw new IllegalArgumentException("overlap 需满足 0 <= overlap < chunkSize");
        }

        List<String> chunks = new ArrayList<>();
        for (String line : text.split("\\R")) {
            String paragraph = line.trim().replaceAll("[ \\t]+", " ");
            if (paragraph.isEmpty()) {
                continue;
            }
            if (paragraph.length() <= chunkSize) {
                chunks.add(paragraph);
            } else {
                chunks.addAll(slidingWindow(paragraph, chunkSize, overlap));
            }
        }
        return chunks;
    }

    private static List<String> slidingWindow(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int step = chunkSize - overlap;
        for (int start = 0; start < text.length(); start += step) {
            int end = Math.min(start + chunkSize, text.length());
            String piece = text.substring(start, end).trim();
            if (!piece.isEmpty()) {
                chunks.add(piece);
            }
            if (end >= text.length()) {
                break;
            }
        }
        return chunks;
    }
}

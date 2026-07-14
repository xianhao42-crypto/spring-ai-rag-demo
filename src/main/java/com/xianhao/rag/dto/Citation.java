package com.xianhao.rag.dto;

/**
 * 单条引用片段。
 *
 * @author 13108
 */
public record Citation(
        int index,
        String id,
        String content,
        Double score,
        String source,
        Integer chunkIndex
) {
}

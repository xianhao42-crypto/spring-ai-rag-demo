package com.xianhao.rag.dto;

/**
 * @author 13108
 */
public record DocumentRow(
        String id,
        String content,
        String source,
        String docId,
        Integer chunkIndex
) {
}

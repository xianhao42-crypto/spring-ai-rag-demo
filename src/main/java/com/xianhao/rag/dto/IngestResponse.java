package com.xianhao.rag.dto;

import java.util.List;

/**
 * @author 13108
 */
public record IngestResponse(
        String docId,
        String source,
        int chunkCount,
        List<String> chunkIds,
        long costMs
) {
}

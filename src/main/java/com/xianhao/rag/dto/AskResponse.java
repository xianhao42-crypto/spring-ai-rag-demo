package com.xianhao.rag.dto;

import java.util.List;

/**
 * RAG 问答响应：答案 + 引用。
 *
 * @author 13108
 */
public record AskResponse(
        String question,
        String answer,
        boolean grounded,
        List<Citation> citations,
        long retrieveMs,
        long generateMs,
        long totalMs
) {
}

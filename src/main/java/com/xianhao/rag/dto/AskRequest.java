package com.xianhao.rag.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @author 13108
 */
public record AskRequest(
        @NotBlank(message = "question 不能为空") String question,
        Integer topK,
        Double similarityThreshold
) {
}

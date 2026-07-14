package com.xianhao.rag.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @author 13108
 */
public record IngestTextRequest(
        @NotBlank(message = "text 不能为空") String text,
        String source
) {
}

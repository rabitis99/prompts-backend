package org.example.sharedprompts.domain.prompt.application.port.in.query;

import java.util.List;

/** 프롬프트 페이지 결과 */
public record PromptPageResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public PromptPageResult {
        content = content == null ? List.of() : List.copyOf(content);
    }
}


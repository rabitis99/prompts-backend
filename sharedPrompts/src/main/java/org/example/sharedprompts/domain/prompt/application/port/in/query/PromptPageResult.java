package org.example.sharedprompts.domain.prompt.application.port.in.query;

import java.util.List;

/**
 * 프롬프트 페이지 결과 모델.
 *
 * <p>웹 전송용 PageResponse 와는 분리된 애플리케이션 계층 전용 페이지 래퍼.</p>
 */
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


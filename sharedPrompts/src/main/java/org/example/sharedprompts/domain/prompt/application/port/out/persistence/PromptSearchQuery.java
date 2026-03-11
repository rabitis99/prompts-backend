package org.example.sharedprompts.domain.prompt.application.port.out.persistence;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.sort.SortType;

/** 프롬프트 검색 쿼리. keyword는 제목·설명·태그 텍스트 검색에 사용 */
public record PromptSearchQuery(
        int page,
        int size,
        SortType sort,
        PromptCategory category,
        Long ownerId,
        Long viewerId,
        String keyword
) {
}


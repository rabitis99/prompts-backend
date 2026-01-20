package org.example.sharedprompts.domain.prompt.repository;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.dto.prompt.request.PromptSearchCondition;

/**
 * 프롬프트 검색 시 사용되는 컨텍스트 래퍼.
 *
 * - 검색 조건(PromptSearchCondition)과 viewerId(보안/차단 컨텍스트)를 분리한다.
 * - Repository 계층에서는 이 컨텍스트만을 받아 필요한 정보를 사용한다.
 */
@Getter
@RequiredArgsConstructor
public class PromptSearchContext {

    /**
     * 검색 조건 (클라이언트 요청 파라미터 기반)
     */
    private final PromptSearchCondition condition;

    /**
     * 조회자 ID (SecurityContext 기반)
     * - BLOCKED 필터링 등 viewer 컨텍스트가 필요한 조건에만 사용한다.
     */
    private final Long viewerId;

    public static PromptSearchContext of(PromptSearchCondition condition, Long viewerId) {
        return new PromptSearchContext(condition, viewerId);
    }
}



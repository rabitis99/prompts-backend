package org.example.sharedprompts.domain.prompt.repository;

import lombok.Getter;
import org.example.sharedprompts.dto.prompt.request.PromptSearchCondition;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;

/**
 * 프롬프트 검색 시 사용되는 컨텍스트 래퍼.
 *
 * - 검색 조건(PromptSearchCondition)과 viewerId(보안/차단 컨텍스트)를 분리한다.
 * - Repository 계층에서는 이 컨텍스트만을 받아 필요한 정보를 사용한다.
 * - 생성자는 private으로 제한하여 of() 팩토리 메서드를 통해서만 생성 가능합니다.
 */
@Getter
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

    /**
     * Private 생성자 - of() 팩토리 메서드를 통해서만 인스턴스 생성 가능
     * 
     * @param condition 검색 조건 (null이면 안 됨)
     * @param viewerId 조회자 ID (null 가능)
     * @throws ApiException condition이 null인 경우
     */
    private PromptSearchContext(PromptSearchCondition condition, Long viewerId) {
        if (condition == null) {
            throw new ApiException(ErrorCode.PROMPT_SEARCH_CONDITION_REQUIRED);
        }
        this.condition = condition;
        this.viewerId = viewerId;
    }

    /**
     * PromptSearchContext 팩토리 메서드
     * 
     * @param condition 검색 조건 (null이면 안 됨)
     * @param viewerId 조회자 ID (null 가능)
     * @return PromptSearchContext 인스턴스
     * @throws ApiException condition이 null인 경우
     */
    public static PromptSearchContext of(PromptSearchCondition condition, Long viewerId) {
        return new PromptSearchContext(condition, viewerId);
    }
}



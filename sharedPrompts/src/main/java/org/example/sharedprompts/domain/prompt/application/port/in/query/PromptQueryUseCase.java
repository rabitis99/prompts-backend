package org.example.sharedprompts.domain.prompt.application.port.in.query;

/**
 * 프롬프트 조회 관련 유즈케이스 포트.
 *
 * <p>웹 DTO나 전송 계층 타입에 의존하지 않고,
 * 도메인 전용 View / PageResult 모델만을 노출한다.</p>
 */
public interface PromptQueryUseCase {

    /**
     * 검색 조건에 맞는 프롬프트 목록을 페이지 단위로 반환한다.
     */
    PromptPageResult<PromptSummaryView> getPrompts(SearchPromptsQuery query);

    /**
     * 단건 상세를 반환한다. viewerId는 조회수·권한 판단에 사용된다.
     */
    PromptDetailView getPromptDetail(Long promptId, Long viewerId);

    /**
     * 현재 사용자(ownerId)가 소유한 프롬프트 목록을 반환한다.
     */
    PromptPageResult<PromptSummaryView> getMyPrompts(SearchPromptsQuery query);

    /**
     * 지정 사용자의 공개 프롬프트 목록을 반환한다.
     */
    PromptPageResult<PromptSummaryView> getUserPrompts(SearchPromptsQuery query);
}


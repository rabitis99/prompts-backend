package org.example.sharedprompts.domain.prompt.event;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 프롬프트 관련 도메인 이벤트
 * 
 * <p>통계 캐시 무효화, 알림 발송 등 다양한 후처리를 위한 이벤트를 정의합니다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PromptEvent {

    /**
     * 프롬프트 생성 이벤트
     * 
     * @param promptId 생성된 프롬프트 ID
     * @param userId 프롬프트 작성자 ID
     */
    public record Created(Long promptId, Long userId) {}

    /**
     * 프롬프트 삭제 이벤트
     * 
     * @param promptId 삭제된 프롬프트 ID
     * @param userId 프롬프트 작성자 ID
     */
    public record Deleted(Long promptId, Long userId) {}

    /**
     * 프롬프트 조회 이벤트
     * 
     * @param promptId 조회된 프롬프트 ID
     * @param viewerId 조회자 ID (nullable - 비로그인 사용자)
     */
    public record Viewed(Long promptId, Long viewerId) {}
}


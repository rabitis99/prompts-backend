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
     */
    public record Created(Long promptId, Long userId) {}

    /**
     * 프롬프트 삭제 이벤트
     */
    public record Deleted(Long promptId, Long userId) {}

    /**
     * 프롬프트 조회 이벤트
     */
    public record Viewed(Long promptId, Long viewerId) {}
}


package org.example.sharedprompts.domain.prompt.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 프롬프트 관련 이벤트 발행자
 * 
 * <p>프롬프트 도메인 이벤트를 발행하여 통계 캐시 무효화, 알림 발송 등의
 * 후처리를 수행할 수 있도록 합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromptEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 프롬프트 생성 이벤트 발행
     * 
     * @param promptId 생성된 프롬프트 ID
     * @param userId 프롬프트 작성자 ID
     */
    public void publishPromptCreated(Long promptId, Long userId) {
        PromptEvent.Created event = new PromptEvent.Created(promptId, userId);
        eventPublisher.publishEvent(event);
        log.debug("Published PromptEvent.Created: promptId={}, userId={}", promptId, userId);
    }

    /**
     * 프롬프트 삭제 이벤트 발행
     * 
     * @param promptId 삭제된 프롬프트 ID
     * @param userId 프롬프트 작성자 ID
     */
    public void publishPromptDeleted(Long promptId, Long userId) {
        PromptEvent.Deleted event = new PromptEvent.Deleted(promptId, userId);
        eventPublisher.publishEvent(event);
        log.debug("Published PromptEvent.Deleted: promptId={}, userId={}", promptId, userId);
    }
}


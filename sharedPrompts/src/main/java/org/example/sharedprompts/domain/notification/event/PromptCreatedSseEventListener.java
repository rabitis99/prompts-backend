package org.example.sharedprompts.domain.notification.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.notification.service.NotificationSseService;
import org.example.sharedprompts.domain.notification.service.NotificationTargetService;
import org.example.sharedprompts.domain.prompt.event.PromptCreatedEvent;
import org.example.sharedprompts.dto.notification.response.PromptCreatedSsePayload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * 프롬프트 생성 후 팔로워에게 SSE 알림을 전송하는 리스너
 *
 * - PromptCreatedEvent 를 수신
 * - 트랜잭션 커밋 AFTER_COMMIT 시점에 실행
 * - 팔로잉(FOLLOWING) 중이며 BLOCKED 관계가 아닌 사용자에게만 알림 전송
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PromptCreatedSseEventListener {

    private final NotificationTargetService notificationTargetService;
    private final NotificationSseService sseService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePromptCreated(PromptCreatedEvent event) {
        Long authorId = event.getAuthorId();
        if (authorId == null) {
            log.warn("PromptCreatedEvent with null authorId: promptId={}", event.getPromptId());
            return;
        }

        // 알림 대상 정책(Service)에서 FOLLOWING + NOT BLOCKED 조건을 조합하여 대상 ID 목록 조회
        List<Long> followerIds = notificationTargetService.getPromptCreatedTargets(authorId);

        if (followerIds.isEmpty()) {
            return;
        }

        PromptCreatedSsePayload payload = PromptCreatedSsePayload.builder()
                .promptId(event.getPromptId())
                .authorId(authorId)
                .authorNickname(event.getAuthorNickname())
                .promptSummary(event.getPromptSummary())
                .build();

        for (Long followerId : followerIds) {
            try {
                sseService.sendPromptCreated(followerId, payload);
            } catch (Exception e) {
                // SSE 전송 실패는 프롬프트 생성 트랜잭션에 영향을 주지 않도록 로깅만 수행
                log.warn("Failed to send prompt-created SSE to user {} for prompt {}",
                        followerId, event.getPromptId(), e);
            }
        }
    }
}



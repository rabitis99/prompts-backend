package org.example.sharedprompts.domain.notification.validator;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.notification.message.PromptCreatedMessage;
import org.springframework.stereotype.Component;

/**
 * 프롬프트 생성 메시지 검증기
 * - 메시지 유효성 검증 로직 분리
 */
@Slf4j
@Component
public class PromptCreatedMessageValidator {

    /**
     * 메시지 유효성 검증
     *
     * @param message 검증할 메시지
     * @return 유효한 경우 true, 그렇지 않으면 false
     */
    public boolean isValid(PromptCreatedMessage message) {
        if (message == null) {
            log.warn("PromptCreatedMessage is null");
            return false;
        }

        if (message.getPromptId() == null) {
            log.warn("PromptCreatedMessage has null promptId");
            return false;
        }

        if (message.getAuthorId() == null) {
            log.warn("PromptCreatedMessage has null authorId");
            return false;
        }

        if (message.getFollowerIds() == null || message.getFollowerIds().isEmpty()) {
            log.debug("PromptCreatedMessage has no followers: promptId={}", message.getPromptId());
            return false;
        }

        return true;
    }

    /**
     * 알림 전송이 필요한지 확인
     *
     * @param message 메시지
     * @return 전송이 필요한 경우 true
     */
    public boolean shouldSend(PromptCreatedMessage message) {
        return isValid(message);
    }
}


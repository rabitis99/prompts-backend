package org.example.sharedprompts.domain.notification.factory;

import org.example.sharedprompts.domain.notification.message.PromptCreatedMessage;
import org.example.sharedprompts.dto.notification.response.PromptCreatedSsePayload;
import org.springframework.stereotype.Component;

/**
 * 프롬프트 생성 SSE 페이로드를 생성하는 Factory
 * - RabbitMQ 메시지에서 SSE 페이로드로 변환
 */
@Component
public class PromptCreatedSsePayloadFactory {

    /**
     * RabbitMQ 메시지로부터 SSE 페이로드 생성
     *
     * @param message 프롬프트 생성 SSE 알림 메시지
     * @return SSE 페이로드
     */
    public PromptCreatedSsePayload createPayload(PromptCreatedMessage message) {
        return PromptCreatedSsePayload.builder()
                .promptId(message.getPromptId())
                .authorId(message.getAuthorId())
                .authorNickname(message.getAuthorNickname())
                .promptSummary(message.getPromptSummary())
                .build();
    }
}


package org.example.sharedprompts.domain.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.notification.factory.PromptCreatedMessageFactory;
import org.example.sharedprompts.domain.notification.message.PromptCreatedMessage;
import org.example.sharedprompts.domain.notification.producer.PromptCreatedSseProducer;
import org.example.sharedprompts.domain.prompt.entity.Prompt;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.global.util.TransactionUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executor;

/**
 * 프롬프트 생성 이벤트 발행 서비스
 * - 트랜잭션 커밋 후 RabbitMQ로 SSE 알림 메시지 발행
 * - 프롬프트 생성과 SSE 알림 발행을 분리
 * - 비동기 실행으로 HTTP 응답 스레드 블로킹 방지
 */
@Slf4j
@Service
public class PromptCreatedEventPublisher {

    private final PromptCreatedMessageFactory messageFactory;
    private final PromptCreatedSseProducer producer;
    private final Executor sseTaskExecutor;

    public PromptCreatedEventPublisher(
            PromptCreatedMessageFactory messageFactory,
            PromptCreatedSseProducer producer,
            @Qualifier("sseTaskExecutor") Executor sseTaskExecutor) {
        this.messageFactory = messageFactory;
        this.producer = producer;
        this.sseTaskExecutor = sseTaskExecutor;
    }

    /**
     * 프롬프트 생성 이벤트 발행
     * - 트랜잭션 커밋 후 실행되도록 등록
     * - 팔로워가 없으면 메시지 발행하지 않음
     *
     * @param prompt 생성된 프롬프트 엔티티
     * @param author 프롬프트 작성자
     */
    public void publishAfterCommit(Prompt prompt, User author) {
        TransactionUtils.executeAfterCommit(() -> {
            try {
                PromptCreatedMessage message = messageFactory.createMessage(prompt, author);
                if (message != null) {
                    // 비동기로 실행하여 HTTP 응답 스레드 블로킹 방지
                    sseTaskExecutor.execute(() -> {
                        try {
                            producer.publishPromptCreated(message);
                            log.debug("Prompt created SSE message queued for RabbitMQ: promptId={}, followerCount={}", 
                                    prompt.getId(), message.getFollowerIds().size());
                        } catch (Exception e) {
                            log.error("Failed to publish prompt created SSE message asynchronously: promptId={}, authorId={}", 
                                    prompt.getId(), author.getId(), e);
                        }
                    });
                }
            } catch (Exception e) {
                // 이미 TransactionUtils에서 예외를 잡아 로깅하므로 여기서는 재발생만 하지 않음
                log.error("Failed to create prompt created message: promptId={}, authorId={}", 
                        prompt.getId(), author.getId(), e);
            }
        });
    }

    /**
     * 프롬프트 생성 이벤트 발행 (엔티티가 아닌 값으로)
     * - 트랜잭션 커밋 후 실행되도록 등록
     *
     * @param promptId 프롬프트 ID
     * @param authorId 작성자 ID
     * @param authorNickname 작성자 닉네임
     * @param promptSummary 프롬프트 요약
     */
    public void publishAfterCommit(Long promptId, Long authorId, 
                                    String authorNickname, String promptSummary) {
        TransactionUtils.executeAfterCommit(() -> {
            try {
                PromptCreatedMessage message = messageFactory.createMessage(
                        promptId, authorId, authorNickname, promptSummary);
                if (message != null) {
                    // 비동기로 실행하여 HTTP 응답 스레드 블로킹 방지
                    sseTaskExecutor.execute(() -> {
                        try {
                            producer.publishPromptCreated(message);
                            log.debug("Prompt created SSE message queued for RabbitMQ: promptId={}, followerCount={}", 
                                    promptId, message.getFollowerIds().size());
                        } catch (Exception e) {
                            log.error("Failed to publish prompt created SSE message asynchronously: promptId={}, authorId={}", 
                                    promptId, authorId, e);
                        }
                    });
                }
            } catch (Exception e) {
                log.error("Failed to create prompt created message: promptId={}, authorId={}", 
                        promptId, authorId, e);
            }
        });
    }
}


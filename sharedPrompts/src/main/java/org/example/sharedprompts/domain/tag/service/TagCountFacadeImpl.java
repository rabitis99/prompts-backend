package org.example.sharedprompts.domain.tag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.tag.count.TagCountUpdateService;
import org.example.sharedprompts.domain.tag.event.TagEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Set;

/**
 * 태그 카운트 파사드 구현체
 * - TagCountUpdateService와 TagEventPublisher를 통합하여 단순한 인터페이스 제공
 * - 트랜잭션 처리 로직 캡슐화
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TagCountFacadeImpl implements TagCountFacade {

    private final TagCountUpdateService tagCountUpdateService;
    private final TagEventPublisher tagEventPublisher;

    @Value("${tag.count.update.sync-fallback:false}")
    private boolean syncFallback;

    @Override
    public void incrementTagCount(String tagName) {
        // 트랜잭션 내부에서 즉시 처리
        tagCountUpdateService.incrementTagCount(tagName);
    }

    @Override
    public void publishTagCountUpdate(Set<String> tagsToDecrease, Set<String> tagsToIncrease) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            // 트랜잭션이 있는 경우: 커밋 후 비동기 처리
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            tagEventPublisher.publishTagCountUpdate(tagsToDecrease, tagsToIncrease);
                            log.debug("Tag count update event published after commit: " +
                                    "decrease={}, increase={}", 
                                    tagsToDecrease.size(), tagsToIncrease.size());
                        }
                    }
            );
        } else {
            // 트랜잭션이 없는 경우
            if (syncFallback) {
                // 동기 처리 모드: 즉시 처리 (테스트/배치 환경)
                log.debug("Sync fallback mode: processing tag count update synchronously");
                tagEventPublisher.publishTagCountUpdate(tagsToDecrease, tagsToIncrease);
            } else {
                // 비동기 처리 모드: 이벤트 발행 (기본값)
                log.debug("Async fallback mode: publishing tag count update event");
                tagEventPublisher.publishTagCountUpdate(tagsToDecrease, tagsToIncrease);
            }
        }
    }
}


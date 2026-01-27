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

    /**
     * 단일 태그 카운트 즉시 증가
     *
     * ⚠️ 주의: 이 메서드는 트랜잭션 커밋 여부와 관계없이 Redis 카운트를 즉시 증가시킵니다.
     * 트랜잭션이 롤백되면 DB와 Redis 간 불일치가 발생할 수 있습니다.
     *
     * 권장: 트랜잭션 내부에서 사용하는 경우 {@link #publishTagCountUpdate(Set, Set)}를 사용하세요.
     * 이 메서드는 비트랜잭션 컨텍스트(예: 배치 작업, 수동 동기화)에서만 사용해야 합니다.
     *
     * @param tagName 태그 이름
     * @deprecated 트랜잭션 안전성을 위해 {@link #publishTagCountUpdate(Set, Set)} 사용을 권장합니다.
     */
    @Override
    @Deprecated
    public void incrementTagCount(String tagName) {
        // 트랜잭션 내부에서 즉시 처리 (주의: 롤백 시 불일치 가능)
        tagCountUpdateService.incrementTagCount(tagName);
    }

    @Override
    public void publishTagCountUpdate(Set<String> tagsToDecrease, Set<String> tagsToIncrease) {
        // 호출 시점에 Set이 변경될 수 있으므로 스냅샷 복사
        Set<String> decreaseSnapshot = Set.copyOf(tagsToDecrease);
        Set<String> increaseSnapshot = Set.copyOf(tagsToIncrease);

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            // 트랜잭션이 있는 경우: 커밋 후 비동기 처리
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            tagEventPublisher.publishTagCountUpdate(decreaseSnapshot, increaseSnapshot);
                            log.debug("Tag count update event published after commit: " +
                                            "decrease={}, increase={}",
                                    decreaseSnapshot.size(), increaseSnapshot.size());
                        }
                    }
            );
        } else {
            // 트랜잭션이 없는 경우
            if (syncFallback) {
                // 동기 처리 모드: 즉시 처리 (테스트/배치 환경)
                log.debug("Sync fallback mode: processing tag count update synchronously");
                tagCountUpdateService.updateTagCounts(decreaseSnapshot, increaseSnapshot);
            } else {
                // 비동기 처리 모드: 이벤트 발행 (기본값)
                log.debug("Async fallback mode: publishing tag count update event");
                tagEventPublisher.publishTagCountUpdate(decreaseSnapshot, increaseSnapshot);
            }
        }
    }
}

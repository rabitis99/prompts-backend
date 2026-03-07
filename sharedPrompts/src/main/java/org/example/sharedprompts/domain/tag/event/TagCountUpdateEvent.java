package org.example.sharedprompts.domain.tag.event;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;

/**
 * 태그 카운트 업데이트 이벤트
 * - 트랜잭션 커밋 후 Redis 카운트 업데이트를 위한 이벤트
 * - 이벤트 기반 아키텍처로 확장 가능 (Kafka, Redis PubSub 등)
 */
@Value
@Builder
public class TagCountUpdateEvent {
    
    /**
     * 제거된 태그 이름 목록 (카운트 감소 대상)
     */
    Set<String> tagsToDecrease;
    
    /**
     * 추가된 태그 이름 목록 (카운트 증가 대상)
     */
    Set<String> tagsToIncrease;
    
    /**
     * 이벤트 발생 시각
     */
    @Builder.Default
    LocalDateTime occurredAt = LocalDateTime.now(ZoneOffset.UTC);
    
    /**
     * 재시도 횟수 (실패 시 증가)
     */
    @Builder.Default
    int retryCount = 0;

}


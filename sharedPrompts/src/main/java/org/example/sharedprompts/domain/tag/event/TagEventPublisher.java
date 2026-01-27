package org.example.sharedprompts.domain.tag.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 태그 관련 이벤트 발행자
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TagEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 태그 카운트 업데이트 이벤트 발행
     * 
     * @param tagsToDecrease 제거된 태그 이름 목록
     * @param tagsToIncrease 추가된 태그 이름 목록
     */
    public void publishTagCountUpdate(Set<String> tagsToDecrease, Set<String> tagsToIncrease) {
        TagCountUpdateEvent event = TagCountUpdateEvent.builder()
                .tagsToDecrease(tagsToDecrease)
                .tagsToIncrease(tagsToIncrease)
                .build();
        
        eventPublisher.publishEvent(event);
        log.debug("Published TagCountUpdateEvent: decrease={}, increase={}", 
                tagsToDecrease.size(), tagsToIncrease.size());
    }
}


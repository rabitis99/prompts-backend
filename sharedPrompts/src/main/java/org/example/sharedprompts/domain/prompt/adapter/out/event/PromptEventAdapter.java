package org.example.sharedprompts.domain.prompt.adapter.out.event;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.application.port.out.event.PromptDeletedEvent;
import org.example.sharedprompts.domain.prompt.application.port.out.event.PromptEventPort;
import org.example.sharedprompts.domain.prompt.application.port.out.event.PromptViewedEvent;
import org.example.sharedprompts.domain.prompt.event.PromptEventPublisher;
import org.springframework.stereotype.Component;

// 의존성 방향 어댑터 (의미 변환 없음)
/** 이벤트 인프라로 연결하는 어댑터. 프롬프트 도메인 이벤트를 변환 없이 전달한다. */
@Component
@RequiredArgsConstructor
public class PromptEventAdapter implements PromptEventPort {

    private final PromptEventPublisher promptEventPublisher;

    @Override
    public void publishPromptViewed(PromptViewedEvent event) {
        promptEventPublisher.publishPromptViewed(event.promptId(), event.viewerId());
    }

    @Override
    public void publishPromptDeleted(PromptDeletedEvent event) {
        promptEventPublisher.publishPromptDeleted(event.promptId(), event.authorId());
    }
}
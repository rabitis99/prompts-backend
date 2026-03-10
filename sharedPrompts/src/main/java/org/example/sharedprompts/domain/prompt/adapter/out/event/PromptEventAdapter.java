package org.example.sharedprompts.domain.prompt.adapter.out.event;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.application.port.out.event.PromptEventPort;
import org.example.sharedprompts.domain.prompt.event.PromptEventPublisher;
import org.springframework.stereotype.Component;

// Dependency-direction adapter (no semantic translation)
/** Adapter: bridges to event infrastructure. Forwards prompt-domain events without translation. */
@Component
@RequiredArgsConstructor
public class PromptEventAdapter implements PromptEventPort {

    private final PromptEventPublisher promptEventPublisher;

    @Override
    public void publishPromptViewed(Long promptId, Long viewerId) {
        promptEventPublisher.publishPromptViewed(promptId, viewerId);
    }

    @Override
    public void publishPromptDeleted(Long promptId, Long authorId) {
        promptEventPublisher.publishPromptDeleted(promptId, authorId);
    }
}

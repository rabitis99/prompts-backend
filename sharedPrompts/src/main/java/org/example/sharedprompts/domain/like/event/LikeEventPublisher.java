package org.example.sharedprompts.domain.like.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LikeEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishPromptLiked(Long userId, Long promptId) {
        eventPublisher.publishEvent(new LikeEvent.PromptLiked(userId, promptId));
    }

    public void publishCommentLiked(Long userId, Long commentId) {
        eventPublisher.publishEvent(new LikeEvent.CommentLiked(userId, commentId));
    }
}



package org.example.sharedprompts.domain.comment.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.comment.Comment;
import org.example.sharedprompts.domain.comment.event.CommentEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentEventPublisher {

    private final ApplicationEventPublisher publisher;

    public void publishCreated(Comment comment, Long parentId) {
        publisher.publishEvent(new CommentEvent.Created(
                comment.getId(),
                comment.getUser().getId(),
                comment.getPrompt().getId(),
                parentId
        ));
    }

    public void publishDeleted(Long promptId, Long parentId) {
        publisher.publishEvent(new CommentEvent.Deleted(
                promptId,
                parentId
        ));
    }
}



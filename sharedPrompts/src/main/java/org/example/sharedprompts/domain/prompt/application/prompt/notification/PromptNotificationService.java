package org.example.sharedprompts.domain.prompt.application.prompt.notification;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.notification.service.PromptCreatedEventPublisher;
import org.example.sharedprompts.domain.prompt.entity.Prompt;
import org.example.sharedprompts.domain.user.User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PromptNotificationService {

    private final PromptCreatedEventPublisher promptCreatedEventPublisher;

    public void publishPromptCreated(Prompt prompt, User user) {
        promptCreatedEventPublisher.publishAfterCommit(prompt, user);
    }
}

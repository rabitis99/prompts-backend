package org.example.sharedprompts.domain.prompt.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.notification.service.PromptCreatedEventPublisher;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.domain.user.User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PromptNotificationService {

    private final PromptCreatedEventPublisher promptCreatedEventPublisher;

    /**
     * 프롬프트 생성 이후의 이벤트 발행, SSE 등 후처리를 담당한다.
     */
    public void publishPromptCreated(Prompt prompt, User user) {
        // 트랜잭션 커밋 이후 발행되도록 Publisher에 위임
        promptCreatedEventPublisher.publishAfterCommit(prompt, user);
    }
}



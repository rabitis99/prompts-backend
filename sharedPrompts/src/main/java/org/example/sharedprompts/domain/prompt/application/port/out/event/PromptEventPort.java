package org.example.sharedprompts.domain.prompt.application.port.out.event;

/** 프롬프트 이벤트 발행 아웃바운드 포트 */
public interface PromptEventPort {

    void publishPromptViewed(Long promptId, Long viewerId);

    void publishPromptDeleted(Long promptId, Long authorId);
}

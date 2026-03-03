package org.example.sharedprompts.domain.prompt.application.port.out.persistence;

import org.example.sharedprompts.domain.prompt.entity.Prompt;

/**
 * 프롬프트 저장·삭제용 아웃바운드 포트.
 */
public interface PromptCommandPort {

    Prompt save(Prompt prompt);

    void delete(Prompt prompt);
}

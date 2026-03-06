package org.example.sharedprompts.domain.prompt.application.port.out.persistence;

import org.example.sharedprompts.domain.prompt.entity.Prompt;

/**
 * 프롬프트 저장·삭제용 아웃바운드 포트.
 *
 * <p>엔티티 단위 저장/삭제만 담당한다. 버전 저장은 {@link SavePromptVersionPort}.</p>
 */
public interface PromptCommandPort {

    /**
     * 프롬프트를 저장(신규/수정)하고 저장된 엔티티를 반환한다.
     */
    Prompt save(Prompt prompt);

    /**
     * 프롬프트를 삭제한다.
     */
    void delete(Prompt prompt);
}

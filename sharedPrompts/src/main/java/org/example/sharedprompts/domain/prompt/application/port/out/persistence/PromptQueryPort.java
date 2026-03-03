package org.example.sharedprompts.domain.prompt.application.port.out.persistence;

import org.example.sharedprompts.domain.prompt.entity.Prompt;
import org.example.sharedprompts.dto.prompt.request.PromptSearchCondition;
import org.springframework.data.domain.Page;

import java.util.Optional;

/**
 * 프롬프트 조회용 아웃바운드 포트.
 */
public interface PromptQueryPort {

    Optional<Prompt> findById(Long promptId);

    Page<Prompt> searchPrompts(PromptSearchCondition condition, Long viewerId);

    Page<Prompt> searchMyPrompts(Long userId, PromptSearchCondition condition);

    Page<Prompt> searchUserPrompts(Long userId, PromptSearchCondition condition, Long viewerId);
}

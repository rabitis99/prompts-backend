package org.example.sharedprompts.domain.prompt.repository;

import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.dto.prompt.request.PromptSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomPromptRepository {
    Page<Prompt> searchPrompts(PromptSearchContext context);
    Page<Prompt> searchMyPrompts(Long userId, PromptSearchCondition condition);
    Page<Prompt> searchUserPrompts(Long userId, PromptSearchCondition condition, Long viewerId);
    Page<Prompt> searchPromptsForAdmin(String keyword, Pageable pageable);
}

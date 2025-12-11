package org.example.sharedprompts.domain.prompt.repository;

import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.dto.prompt.request.PromptSearchCondition;
import org.springframework.data.domain.Page;

public interface CustomPromptRepository {
    Page<Prompt> searchPrompts(PromptSearchCondition condition);
    Page<Prompt> searchMyPrompts(Long userId, PromptSearchCondition condition);
}

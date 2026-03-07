package org.example.sharedprompts.domain.prompt.infrastructure.persistence;

import org.example.sharedprompts.domain.prompt.application.port.out.persistence.PromptSearchQuery;
import org.example.sharedprompts.domain.prompt.entity.Prompt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomPromptRepository {

    Page<Prompt> search(PromptSearchQuery query);

    Page<Prompt> searchPromptsForAdmin(String keyword, Pageable pageable);
}

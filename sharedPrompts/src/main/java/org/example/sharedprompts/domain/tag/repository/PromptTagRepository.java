package org.example.sharedprompts.domain.tag.repository;

import org.example.sharedprompts.domain.tag.PromptTag;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface PromptTagRepository extends JpaRepository<PromptTag, Long> {
    @Modifying
    void deletePromptTagByPrompt(Prompt prompt);

    List<PromptTag> findPromptTagByPrompt(Prompt prompt);
}

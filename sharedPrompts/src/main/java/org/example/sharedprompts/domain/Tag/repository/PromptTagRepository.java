package org.example.sharedprompts.domain.Tag.repository;

import org.example.sharedprompts.domain.Tag.PromptTag;
import org.example.sharedprompts.domain.Tag.Tag;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromptTagRepository extends JpaRepository<PromptTag, Long> {
    boolean existsByPromptAndTag(Prompt prompt, Tag tag);

    void deletePromptTagByPrompt(Prompt prompt);

    List<PromptTag> findPromptTagByPrompt(Prompt prompt);
}

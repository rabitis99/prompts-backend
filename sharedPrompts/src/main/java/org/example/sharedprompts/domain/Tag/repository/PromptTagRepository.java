package org.example.sharedprompts.domain.Tag.repository;

import org.example.sharedprompts.domain.Tag.PromptTag;
import org.example.sharedprompts.domain.Tag.Tag;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PromptTagRepository extends JpaRepository<PromptTag, Long> {
    boolean existsByPromptAndTag(Prompt prompt, Tag tag);

    @Modifying
    void deletePromptTagByPrompt(Prompt prompt);

    @Query("SELECT pt FROM PromptTag pt JOIN FETCH pt.tag WHERE pt.prompt IN :prompts")
    List<PromptTag> findByPromptIn(@Param("prompts") List<Prompt> prompts);

    List<PromptTag> findPromptTagByPrompt(Prompt prompt);
}

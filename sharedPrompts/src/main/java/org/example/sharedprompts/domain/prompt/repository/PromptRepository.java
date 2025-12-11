package org.example.sharedprompts.domain.prompt.repository;

import org.example.sharedprompts.domain.prompt.Prompt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptRepository extends JpaRepository<Prompt, Long>,CustomPromptRepository {
}

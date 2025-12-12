package org.example.sharedprompts.domain.prompt.repository;

import org.example.sharedprompts.domain.prompt.Prompt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PromptRepository extends JpaRepository<Prompt, Long>,CustomPromptRepository {
    @Query("SELECT p.id FROM Prompt p ORDER BY p.id")
    Page<Long> findAllIds(Pageable pageable);
}

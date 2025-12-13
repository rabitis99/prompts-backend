package org.example.sharedprompts.domain.prompt.repository;

import org.example.sharedprompts.domain.prompt.Prompt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PromptRepository extends JpaRepository<Prompt, Long>,CustomPromptRepository {
    @Query("SELECT p.id FROM Prompt p ORDER BY p.id")
    Page<Long> findAllIds(Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(
            value = "UPDATE prompts SET view_count = view_count + 1 WHERE id = :id",
            nativeQuery = true
    )
    void incrementUsageCount(@Param("id") Long id);
}

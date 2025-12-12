package org.example.sharedprompts.domain.prompt.repository;

import org.example.sharedprompts.domain.prompt.Prompt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface PromptRepository extends JpaRepository<Prompt, Long>,CustomPromptRepository {

    @Query("SELECT p.id FROM Prompt p")
    Page<Long> findAllIds(Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Prompt p SET p.commentCount = :count WHERE p.id = :promptId")
    void updateCommentCount(Long promptId, Long count);
}

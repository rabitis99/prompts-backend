package org.example.sharedprompts.domain.prompt.repository;

import org.example.sharedprompts.domain.prompt.Prompt;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PromptRepository extends JpaRepository<Prompt, Long>,CustomPromptRepository {
    @Query("""
        select p.id
        from Prompt p
        where p.id > :lastId
        order by p.id asc
    """)
    List<Long> findAllIds(@Param("lastId") Long lastId, Pageable pageable);

    default List<Long> findAllIds(Long lastId, int batchSize) {
        return findAllIds(
                lastId,
                PageRequest.of(0, batchSize)
        );
    }

    @Modifying(clearAutomatically = false, flushAutomatically = true)
    @Query(
            value = "UPDATE prompts SET view_count = view_count + 1 WHERE id = :id",
            nativeQuery = true
    )
    void incrementUsageCount(@Param("id") Long id);
}

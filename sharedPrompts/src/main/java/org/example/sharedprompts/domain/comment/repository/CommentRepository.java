package org.example.sharedprompts.domain.comment.repository;

import org.example.sharedprompts.domain.comment.Comment;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Query("""
        SELECT DISTINCT c FROM Comment c
        LEFT JOIN FETCH c.user
        LEFT JOIN FETCH c.children ch
        LEFT JOIN FETCH ch.user
        WHERE c.prompt = :prompt AND c.parent IS NULL
        ORDER BY c.createdAt ASC
    """)
    List<Comment> findAllByPrompt(@Param("prompt") Prompt prompt);

    @Query("SELECT c.id FROM Comment c ORDER BY c.id")
    Page<Long> findAllIds(Pageable pageable);
}

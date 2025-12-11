package org.example.sharedprompts.domain.comment.repository;

import org.example.sharedprompts.domain.comment.Comment;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Query("""
        SELECT c FROM Comment c
        LEFT JOIN FETCH c.children
        WHERE c.prompt = :prompt AND c.parent IS NULL
        ORDER BY c.createdAt ASC
""")
    List<Comment> findAllByPrompt(@Param("prompt") Prompt prompt);
}

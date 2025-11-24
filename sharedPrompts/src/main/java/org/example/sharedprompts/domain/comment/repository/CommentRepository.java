package org.example.sharedprompts.domain.comment.repository;

import org.example.sharedprompts.domain.comment.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}

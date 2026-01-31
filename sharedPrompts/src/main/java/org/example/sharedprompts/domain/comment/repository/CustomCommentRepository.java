package org.example.sharedprompts.domain.comment.repository;

import org.example.sharedprompts.domain.comment.Comment;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomCommentRepository {
    /**
     * 프롬프트의 루트 댓글 목록 조회 (페이징 + fetchJoin 최적화)
     */
    Page<Comment> findRootCommentsByPrompt(Prompt prompt, Pageable pageable);
}

package org.example.sharedprompts.domain.comment.repository;

import org.example.sharedprompts.domain.comment.Comment;
import org.example.sharedprompts.domain.prompt.entity.Prompt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long>, CustomCommentRepository {
    @Query("""
        SELECT DISTINCT c FROM Comment c
        LEFT JOIN FETCH c.user
        LEFT JOIN FETCH c.children ch
        LEFT JOIN FETCH ch.user
        LEFT JOIN FETCH c.prompt p
        LEFT JOIN FETCH p.author
        WHERE c.prompt = :prompt AND c.parent IS NULL
        ORDER BY c.createdAt ASC
    """)
    List<Comment> findAllByPrompt(@Param("prompt") Prompt prompt);

    /**
     * 프롬프트의 루트 댓글 목록 조회 (페이징)
     * 
     * <p>컬렉션 fetch join과 Pageable을 함께 사용하면 Hibernate가 메모리 페이징을 수행하여
     * 페이지 크기와 정렬이 깨질 수 있습니다. 따라서 2-step 페이징을 사용하는
     * {@link CustomCommentRepository#findRootCommentsByPrompt(Prompt, Pageable)}로 위임합니다.
     * 
     * @param prompt 프롬프트
     * @param pageable 페이징 정보
     * @return 루트 댓글 페이지
     */
    default Page<Comment> findAllByPromptPaged(Prompt prompt, Pageable pageable) {
        return findRootCommentsByPrompt(prompt, pageable);
    }

    @Query("""
        select c.id
        from Comment c
        where c.id > :lastId
        order by c.id asc
    """)
    List<Long> findAllIds(
            @Param("lastId") Long lastId,
            Pageable pageable
    );

    default List<Long> findAllIds(Long lastId, int batchSize) {
        return findAllIds(
                lastId,
                PageRequest.of(0, batchSize)
        );
    }
}

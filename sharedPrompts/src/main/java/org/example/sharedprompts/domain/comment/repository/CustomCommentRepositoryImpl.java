package org.example.sharedprompts.domain.comment.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.comment.Comment;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.example.sharedprompts.domain.comment.QComment.comment;
import static org.example.sharedprompts.domain.prompt.QPrompt.prompt;
import static org.example.sharedprompts.domain.user.QUser.user;

@RequiredArgsConstructor
public class CustomCommentRepositoryImpl implements CustomCommentRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 2-step 페이징 + fetchJoin
     * 댓글 목록 조회 시 N+1 문제 해결
     */
    @Override
    public Page<Comment> findRootCommentsByPrompt(Prompt targetPrompt, Pageable pageable) {
        // 1) 페이징 가능한 id 조회
        List<Long> ids = queryFactory
                .select(comment.id)
                .from(comment)
                .where(
                        comment.prompt.eq(targetPrompt),
                        comment.parent.isNull()
                )
                .orderBy(comment.createdAt.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2) 전체 개수 조회
        Long totalCount = queryFactory
                .select(comment.count())
                .from(comment)
                .where(
                        comment.prompt.eq(targetPrompt),
                        comment.parent.isNull()
                )
                .fetchOne();

        long total = totalCount != null ? totalCount : 0L;

        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        // 3) fetch join으로 데이터 조회 (user, prompt, prompt.author 포함)
        List<Comment> fetchedComments = queryFactory
                .selectFrom(comment)
                .leftJoin(comment.user, user).fetchJoin()
                .leftJoin(comment.prompt, prompt).fetchJoin()
                .leftJoin(prompt.author).fetchJoin()
                .leftJoin(comment.children).fetchJoin()
                .where(comment.id.in(ids))
                .fetch();

        // ids 순서대로 content 정렬 (createdAt ASC 순서 유지)
        Map<Long, Comment> commentMap = fetchedComments.stream()
                .collect(Collectors.toMap(Comment::getId, c -> c));

        List<Comment> content = ids.stream()
                .map(commentMap::get)
                .filter(java.util.Objects::nonNull)
                .toList();

        return new PageImpl<>(content, pageable, total);
    }
}

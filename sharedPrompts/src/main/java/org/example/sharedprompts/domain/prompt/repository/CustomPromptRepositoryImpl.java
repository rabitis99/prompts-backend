package org.example.sharedprompts.domain.prompt.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.domain.prompt.enums.PromptCategory;
import org.example.sharedprompts.dto.prompt.request.PromptSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.example.sharedprompts.domain.tag.QPromptTag.promptTag;
import static org.example.sharedprompts.domain.tag.QTag.tag;
import static org.example.sharedprompts.domain.prompt.QPrompt.prompt;
import static org.example.sharedprompts.domain.user.QUser.user;

@RequiredArgsConstructor
public class CustomPromptRepositoryImpl implements CustomPromptRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Prompt> searchPrompts(PromptSearchCondition condition) {
        return searchInternal(applyCategory(condition.getPromptCategory()), condition);
    }

    @Override
    public Page<Prompt> searchMyPrompts(Long userId, PromptSearchCondition condition) {
        BooleanExpression categoryExpr = applyCategory(condition.getPromptCategory());
        BooleanExpression where = (categoryExpr != null)
                ? prompt.author.id.eq(userId).and(categoryExpr)
                : prompt.author.id.eq(userId);

        return searchInternal(where, condition);
    }

    /**
     * 공통 2-step 페이징 + fetchJoin
     */
    private Page<Prompt> searchInternal(BooleanExpression where, PromptSearchCondition condition) {
        PageRequest pageable = PageRequest.of(condition.getPage(), condition.getSize());

        // 1) 페이징 가능한 id 조회
        List<Long> ids = queryFactory
                .select(prompt.id)
                .from(prompt)
                .where(where)
                .orderBy(condition.getSort().toOrderSpecifiers(prompt))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long totalCount = queryFactory
                .select(prompt.count())
                .from(prompt)
                .where(where)
                .fetchOne();

        long total = totalCount != null ? totalCount : 0L;

        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        List<Prompt> content = queryFactory
                .selectFrom(prompt)
                .leftJoin(prompt.promptTags, promptTag).fetchJoin()
                .leftJoin(promptTag.tag, tag).fetchJoin()
                .where(prompt.id.in(ids))
                .orderBy(condition.getSort().toOrderSpecifiers(prompt))
                .fetch();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<Prompt> searchPromptsForAdmin(String keyword, Pageable pageable) {
        BooleanExpression where = buildKeywordCondition(keyword);
        return searchInternalForAdmin(where, pageable);
    }

    /**
     * 관리자용 2-step 페이징 + fetchJoin
     * 제목 또는 작성자 닉네임으로 검색
     */
    private Page<Prompt> searchInternalForAdmin(BooleanExpression where, Pageable pageable) {
        // 1) 페이징 가능한 id 조회
        List<Long> ids = queryFactory
                .select(prompt.id)
                .from(prompt)
                .leftJoin(prompt.author, user)
                .where(where)
                .orderBy(prompt.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2) 전체 개수 조회
        Long totalCount = queryFactory
                .select(prompt.countDistinct())
                .from(prompt)
                .leftJoin(prompt.author, user)
                .where(where)
                .fetchOne();

        long total = totalCount != null ? totalCount : 0L;

        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        // 3) fetch join으로 데이터 조회 (author fetch join 포함)
        List<Prompt> content = queryFactory
                .selectFrom(prompt)
                .leftJoin(prompt.author, user).fetchJoin()
                .where(prompt.id.in(ids))
                .orderBy(prompt.createdAt.desc())
                .fetch();

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 키워드 검색 조건 생성 (제목 또는 작성자 닉네임)
     */
    private BooleanExpression buildKeywordCondition(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return null;
        }
        String lowerKeyword = keyword.toLowerCase();
        return prompt.title.lower().contains(lowerKeyword)
                .or(prompt.author.nickname.lower().contains(lowerKeyword));
    }

    private BooleanExpression applyCategory(PromptCategory category) {
        return category != null ? prompt.promptCategory.eq(category) : null;
    }
}

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

import java.util.List;

import static org.example.sharedprompts.domain.Tag.QPromptTag.promptTag;
import static org.example.sharedprompts.domain.Tag.QTag.tag;
import static org.example.sharedprompts.domain.prompt.QPrompt.prompt;

@RequiredArgsConstructor
public class CustomPromptRepositoryImpl implements CustomPromptRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Prompt> searchPrompts(PromptSearchCondition condition) {
        return searchInternal(applyCategory(condition.getPromptCategory()), condition);
    }

    @Override
    public Page<Prompt> searchMyPrompts(Long userId, PromptSearchCondition condition) {
        BooleanExpression where = prompt.author.id.eq(userId)
                .and(applyCategory(condition.getPromptCategory()));

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

        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // 2) fetchJoin 조회 (Prompt + PromptTag + Tag)
        List<Prompt> content = queryFactory
                .selectFrom(prompt)
                .leftJoin(prompt.promptTags, promptTag).fetchJoin()
                .leftJoin(promptTag.tag, tag).fetchJoin()
                .where(prompt.id.in(ids))
                .orderBy(condition.getSort().toOrderSpecifiers(prompt))
                .fetch();

        // 3) total count 조회
        Long totalCount = queryFactory
                .select(prompt.count())
                .from(prompt)
                .where(where)
                .fetchOne();

        long total = totalCount != null ? totalCount : 0L;

        return new PageImpl<>(content, pageable, total);
    }

    private BooleanExpression applyCategory(PromptCategory category) {
        return category != null ? prompt.promptCategory.eq(category) : null;
    }
}

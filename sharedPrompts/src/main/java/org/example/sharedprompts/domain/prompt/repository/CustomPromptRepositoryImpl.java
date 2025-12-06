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

import static org.example.sharedprompts.domain.prompt.QPrompt.prompt;

@RequiredArgsConstructor
public class CustomPromptRepositoryImpl implements CustomPromptRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Prompt> searchPrompts(PromptSearchCondition condition) {
        PageRequest pageable = PageRequest.of(condition.getPage(), condition.getSize());

        List<Prompt> content = queryFactory
                .selectFrom(prompt)
                .where(applyCategory(condition.getPromptCategory()))
                .orderBy(condition.getSort().toOrderSpecifiers(prompt))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long totalCount = queryFactory
                .select(prompt.count())
                .from(prompt)
                .where(applyCategory(condition.getPromptCategory()))
                .fetchOne();

        long total = totalCount != null ? totalCount : 0L;

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<Prompt> searchMyPrompts(Long userId, PromptSearchCondition condition) {
        PageRequest pageable = PageRequest.of(condition.getPage(), condition.getSize());

        List<Prompt> content = queryFactory
                .selectFrom(prompt)
                .where(prompt.author.id.eq(userId)
                        .and(applyCategory(condition.getPromptCategory())))
                .orderBy(condition.getSort().toOrderSpecifiers(prompt))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long totalCount = queryFactory
                .select(prompt.count())
                .from(prompt)
                .where(prompt.author.id.eq(userId)
                        .and(applyCategory(condition.getPromptCategory())))
                .fetchOne();

        long total = totalCount != null ? totalCount : 0L;

        return new PageImpl<>(content, pageable, total);
    }

    private BooleanExpression applyCategory(PromptCategory category) {
        return category != null ? prompt.promptCategory.eq(category) : null;
    }
}
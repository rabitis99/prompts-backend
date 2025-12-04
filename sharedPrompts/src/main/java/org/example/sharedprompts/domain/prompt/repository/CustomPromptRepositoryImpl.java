package org.example.sharedprompts.domain.prompt.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.domain.prompt.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.enums.SortType;
import org.example.sharedprompts.dto.prompt.request.PromptSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.example.sharedprompts.domain.prompt.QPrompt.prompt;

@RequiredArgsConstructor
public class PromptRepositoryImpl implements CustomPromptRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Prompt> searchPrompts(PromptSearchCondition condition) {
        Sort sort = getSort(condition.getSort());
        PageRequest pageable = PageRequest.of(condition.getPage(), condition.getSize(), sort);

        List<Prompt> content = queryFactory
                .selectFrom(prompt)
                .where(applyCategory(condition.getPromptCategory()))
                .orderBy(sortOrders(sort))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory
                .selectFrom(prompt)
                .where(applyCategory(condition.getPromptCategory()))
                .fetch().size();;

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<Prompt> searchMyPrompts(Long userId, PromptSearchCondition condition) {
        Sort sort = getSort(condition.getSort());
        PageRequest pageable = PageRequest.of(condition.getPage(), condition.getSize(), sort);

        List<Prompt> content = queryFactory
                .selectFrom(prompt)
                .where(prompt.author.id.eq(userId)
                        .and(applyCategory(condition.getPromptCategory())))
                .orderBy(sortOrders(sort))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch().size();

        long total = queryFactory
                .selectFrom(prompt)
                .where(prompt.author.id.eq(userId)
                        .and(applyCategory(condition.getPromptCategory())))
                . countQuery.fetchOne();

        return new PageImpl<>(content, pageable, total);
    }

    private BooleanExpression applyCategory(PromptCategory category) {
        return category != null ? prompt.promptCategory.eq(category) : null;
    }

    /**
     * SortType을 Spring Sort로 변환
     */
    private Sort getSort(SortType sortType) {
        return switch (sortType) {
            case POPULAR -> Sort.by(Sort.Direction.DESC, "likeCount", "createdAt");
            case LATEST, default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    /**
     * Spring Sort → QueryDSL OrderSpecifier 변환
     */
    private com.querydsl.core.types.OrderSpecifier<?>[] sortOrders(Sort sort) {
        return sort.stream()
                .map(order -> order.isAscending()
                        ? prompt.get(order.getProperty()).asc()
                        : prompt.get(order.getProperty()).desc())
                .toArray(Object[]::new);
    }
}

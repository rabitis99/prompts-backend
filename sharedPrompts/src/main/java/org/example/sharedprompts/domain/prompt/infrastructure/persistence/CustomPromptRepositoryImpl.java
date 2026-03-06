package org.example.sharedprompts.domain.prompt.infrastructure.persistence;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.follow.repository.FollowPredicates;
import org.example.sharedprompts.domain.prompt.application.port.out.persistence.PromptSearchQuery;
import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.entity.Prompt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.example.sharedprompts.domain.prompt.entity.QPrompt.prompt;
import static org.example.sharedprompts.domain.tag.QPromptTag.promptTag;
import static org.example.sharedprompts.domain.tag.QTag.tag;
import static org.example.sharedprompts.domain.user.QUser.user;

@RequiredArgsConstructor
public class CustomPromptRepositoryImpl implements CustomPromptRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Prompt> search(PromptSearchQuery query) {
        BooleanExpression where = applyCategory(query.category());

        // keyword가 있으면 제목·설명·태그명 검색 조건 적용
        String keyword = query.keyword();
        if (keyword != null && !keyword.isBlank()) {
            BooleanExpression keywordExpr = buildKeywordConditionForSearch(keyword.trim());
            if (keywordExpr != null) {
                where = (where != null) ? where.and(keywordExpr) : keywordExpr;
            }
        }

        // ownerId가 지정되면 해당 작성자의 프롬프트만 조회
        if (query.ownerId() != null) {
            BooleanExpression ownerExpr = prompt.author.id.eq(query.ownerId());
            where = (where != null) ? where.and(ownerExpr) : ownerExpr;
            // 본인이 아닌 경우 공개 프롬프트만 조회
            if (query.viewerId() == null || !query.ownerId().equals(query.viewerId())) {
                where = where.and(prompt.isPublic.eq(true));
            }
        } else {
            // 공개 피드: 공개 프롬프트만 조회
            where = (where != null) ? where.and(prompt.isPublic.eq(true)) : prompt.isPublic.eq(true);
        }

        return searchInternal(where, query);
    }

    /**
     * 공통 2-step 페이징 + fetchJoin
     */
    private Page<Prompt> searchInternal(BooleanExpression where, PromptSearchQuery query) {
        PageRequest pageable = PageRequest.of(query.page(), query.size());

        // viewer(요청자)와 author 간 BLOCKED 관계가 존재하는 프롬프트는 제외
        Long viewerId = query.viewerId();
        if (viewerId != null) {
            BooleanExpression notBlocked =
                    FollowPredicates.notBlockedBetween(viewerId, prompt.author.id);

            if (notBlocked != null) {
                where = (where != null) ? where.and(notBlocked) : notBlocked;
            }
        }

        // 1) 페이징 가능한 id 조회
        List<Long> ids = queryFactory
                .select(prompt.id)
                .from(prompt)
                .where(where)
                .orderBy(query.sort().toOrderSpecifiers(prompt))
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
                .orderBy(query.sort().toOrderSpecifiers(prompt))
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

    /**
     * 제목·설명·태그명에 대한 키워드 검색 조건 (대소문자 무시, 부분 일치).
     */
    private BooleanExpression buildKeywordConditionForSearch(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return null;
        }
        String lowerKeyword = keyword.toLowerCase();
        BooleanExpression titleOrDesc = prompt.title.lower().contains(lowerKeyword)
                .or(prompt.description.lower().contains(lowerKeyword));
        BooleanExpression tagMatch = prompt.id.in(
                JPAExpressions
                        .select(promptTag.prompt.id)
                        .from(promptTag)
                        .join(promptTag.tag, tag)
                        .where(tag.name.lower().contains(lowerKeyword))
        );
        return titleOrDesc.or(tagMatch);
    }
}

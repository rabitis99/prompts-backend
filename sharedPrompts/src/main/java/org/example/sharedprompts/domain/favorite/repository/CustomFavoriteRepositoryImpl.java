package org.example.sharedprompts.domain.favorite.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.example.sharedprompts.domain.favorite.QFavorite.favorite;
import static org.example.sharedprompts.domain.prompt.QPrompt.prompt;
import static org.example.sharedprompts.domain.tag.QPromptTag.promptTag;
import static org.example.sharedprompts.domain.tag.QTag.tag;

@RequiredArgsConstructor
public class CustomFavoriteRepositoryImpl implements CustomFavoriteRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 2-step 페이징 + fetchJoin
     * 즐겨찾기 목록 조회 시 N+1 문제 해결
     */
    @Override
    public Page<Prompt> findPromptsByUserId(Long userId, Pageable pageable) {
        // 1) 페이징 가능한 prompt id 조회
        List<Long> promptIds = queryFactory
                .select(favorite.prompt.id)
                .from(favorite)
                .where(favorite.user.id.eq(userId))
                .orderBy(favorite.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2) 전체 개수 조회
        Long totalCount = queryFactory
                .select(favorite.count())
                .from(favorite)
                .where(favorite.user.id.eq(userId))
                .fetchOne();

        long total = totalCount != null ? totalCount : 0L;

        if (promptIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        // 3) fetch join으로 데이터 조회 (promptTags와 tag 포함)
        List<Prompt> fetchedPrompts = queryFactory
                .selectFrom(prompt)
                .leftJoin(prompt.promptTags, promptTag).fetchJoin()
                .leftJoin(promptTag.tag, tag).fetchJoin()
                .where(prompt.id.in(promptIds))
                .fetch();

        // promptIds 순서대로 content 정렬 (Favorite.createdAt DESC 순서 유지)
        java.util.Map<Long, Prompt> promptMap = fetchedPrompts.stream()
                .collect(java.util.stream.Collectors.toMap(Prompt::getId, p -> p));
        
        List<Prompt> content = promptIds.stream()
                .map(promptMap::get)
                .filter(java.util.Objects::nonNull)
                .toList();

        return new PageImpl<>(content, pageable, total);
    }
}


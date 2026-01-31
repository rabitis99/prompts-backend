package org.example.sharedprompts.domain.payment.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.UserTierHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.example.sharedprompts.domain.payment.QUserTierHistory.userTierHistory;
import static org.example.sharedprompts.domain.user.QUser.user;

@RequiredArgsConstructor
public class CustomUserTierHistoryRepositoryImpl implements CustomUserTierHistoryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 2-step 페이징 + fetchJoin
     * 티어 변경 이력 조회 시 N+1 문제 해결
     */
    @Override
    public Page<UserTierHistory> findByUserIdWithFetchJoin(Long userId, Pageable pageable) {
        // 1) 페이징 가능한 id 조회
        List<Long> ids = queryFactory
                .select(userTierHistory.id)
                .from(userTierHistory)
                .where(userTierHistory.user.id.eq(userId))
                .orderBy(userTierHistory.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2) 전체 개수 조회
        Long totalCount = queryFactory
                .select(userTierHistory.count())
                .from(userTierHistory)
                .where(userTierHistory.user.id.eq(userId))
                .fetchOne();

        long total = totalCount != null ? totalCount : 0L;

        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        // 3) fetch join으로 데이터 조회 (user 포함)
        List<UserTierHistory> fetchedHistories = queryFactory
                .selectFrom(userTierHistory)
                .leftJoin(userTierHistory.user, user).fetchJoin()
                .where(userTierHistory.id.in(ids))
                .fetch();

        // ids 순서대로 content 정렬 (createdAt DESC 순서 유지)
        Map<Long, UserTierHistory> historyMap = fetchedHistories.stream()
                .collect(Collectors.toMap(UserTierHistory::getId, h -> h));

        List<UserTierHistory> content = ids.stream()
                .map(historyMap::get)
                .filter(java.util.Objects::nonNull)
                .toList();

        return new PageImpl<>(content, pageable, total);
    }
}

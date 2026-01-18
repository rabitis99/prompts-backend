package org.example.sharedprompts.domain.follow.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.follow.FollowStatus;
import org.example.sharedprompts.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.example.sharedprompts.domain.follow.QFollow.follow;
import static org.example.sharedprompts.domain.user.QUser.user;

@Slf4j
@RequiredArgsConstructor
public class CustomFollowRepositoryImpl implements CustomFollowRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 특정 사용자를 팔로우하는 유저 목록 조회 (팔로워 목록)
     * followingId가 userId이고, status가 일치하는 경우
     */
    @Override
    public Page<User> findFollowersByUserIdAndStatus(Long userId, FollowStatus status, Pageable pageable) {
        BooleanExpression whereCondition = buildWhereCondition(null, userId, status);
        return findUsersByCondition(whereCondition, follow.followerId, pageable);
    }

    /**
     * 특정 사용자가 팔로우하는 유저 목록 조회 (팔로잉 목록)
     * followerId가 userId이고, status가 일치하는 경우
     */
    @Override
    public Page<User> findFollowingByUserIdAndStatus(Long userId, FollowStatus status, Pageable pageable) {
        BooleanExpression whereCondition = buildWhereCondition(userId, null, status);
        return findUsersByCondition(whereCondition, follow.followingId, pageable);
    }

    /**
     * 공통 로직: where 조건과 id 경로를 기반으로 User 목록 조회
     */
    private Page<User> findUsersByCondition(
            BooleanExpression whereCondition,
            NumberPath<Long> idPath,
            Pageable pageable
    ) {
        // 1) 팔로우 ID 페이징 조회 (deleted/blocked 유저 제외)
        List<Long> userIds = queryFactory
                .select(idPath)
                .from(follow)
                .join(user).on(idPath.eq(user.id))
                .where(whereCondition
                        .and(user.deletedAt.isNull())
                        .and(user.blocked.isFalse()))
                .orderBy(follow.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2) 전체 count 조회 (Follow + User 조건)
        // userIds가 비어도 offset이 뒤쪽인 경우가 있어 total은 실제 전체 건수여야 함
        BooleanExpression joinCondition = idPath.eq(user.id);
        Long total = queryFactory
                .select(follow.count())
                .from(follow)
                .join(user).on(joinCondition)
                .where(whereCondition
                        .and(user.deletedAt.isNull())
                        .and(user.blocked.isFalse()))
                .fetchOne();

        if (userIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total != null ? total : 0);
        }

        // 3) User 조회 (이미 필터링된 ID들만 조회, 일관성을 위해 필터 재적용)
        List<User> users = queryFactory
                .selectFrom(user)
                .where(user.id.in(userIds)
                        .and(user.deletedAt.isNull())
                        .and(user.blocked.isFalse()))
                .fetch();

        // 4) UserIds 순서대로 정렬
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<User> content = userIds.stream()
                .map(userMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    @Override
    public Long countFollowersByUserIdAndStatus(Long userId, FollowStatus status) {
        BooleanExpression whereCondition = buildWhereCondition(null, userId, status);

        Long count = queryFactory
                .select(follow.count())
                .from(follow)
                .join(user).on(user.id.eq(follow.followerId))
                .where(whereCondition
                        .and(user.deletedAt.isNull())
                        .and(user.blocked.isFalse()))
                .fetchOne();

        return count != null ? count : 0L;
    }

    @Override
    public Long countFollowingByUserIdAndStatus(Long userId, FollowStatus status) {
        BooleanExpression whereCondition = buildWhereCondition(userId, null, status);

        Long count = queryFactory
                .select(follow.count())
                .from(follow)
                .join(user).on(user.id.eq(follow.followingId))
                .where(whereCondition
                        .and(user.deletedAt.isNull())
                        .and(user.blocked.isFalse()))
                .fetchOne();

        return count != null ? count : 0L;
    }

    /**
     * 동적 where 조건 생성
     * QueryDSL의 null 안전 조합 활용
     */
    private BooleanExpression buildWhereCondition(Long followerId, Long followingId, FollowStatus status) {
        // 모든 파라미터가 null이면 false 조건
        if (followerId == null && followingId == null && status == null) {
            log.warn("buildWhereCondition: 모든 파라미터가 null입니다. 전체 테이블 조회를 방지하기 위해 항상 false 조건을 반환합니다.");
            return follow.id.eq(-1L);
        }

        BooleanExpression condition = null;

        if (followerId != null) {
            condition = combine(condition, follow.followerId.eq(followerId));
        }
        if (followingId != null) {
            condition = combine(condition, follow.followingId.eq(followingId));
        }
        if (status != null) {
            condition = combine(condition, follow.status.eq(status));
        }

        return condition;
    }

    /**
     * null-safe BooleanExpression 조합
     */
    private BooleanExpression combine(BooleanExpression condition, BooleanExpression other) {
        if (condition == null) {
            return other;
        }
        if (other == null) {
            return condition;
        }
        return condition.and(other);
    }
}


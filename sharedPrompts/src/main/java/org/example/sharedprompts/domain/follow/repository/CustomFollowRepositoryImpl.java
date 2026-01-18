package org.example.sharedprompts.domain.follow.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.follow.FollowStatus;
import org.example.sharedprompts.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

import static org.example.sharedprompts.domain.follow.QFollow.follow;
import static org.example.sharedprompts.domain.user.QUser.user;

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
        
        // 1) 페이징 가능한 followerId 조회
        List<Long> followerIds = queryFactory
                .select(follow.followerId)
                .from(follow)
                .where(whereCondition)
                .orderBy(follow.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2) 전체 개수 조회
        Long totalCount = queryFactory
                .select(follow.count())
                .from(follow)
                .where(whereCondition)
                .fetchOne();

        long total = totalCount != null ? totalCount : 0L;

        if (followerIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        // 3) User 조회
        List<User> users = queryFactory
                .selectFrom(user)
                .where(user.id.in(followerIds))
                .fetch();

        // followerIds 순서대로 정렬
        Map<Long, User> userMap = users.stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, u -> u));

        List<User> content = followerIds.stream()
                .map(userMap::get)
                .filter(java.util.Objects::nonNull)
                .toList();

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 특정 사용자가 팔로우하는 유저 목록 조회 (팔로잉 목록)
     * followerId가 userId이고, status가 일치하는 경우
     */
    @Override
    public Page<User> findFollowingByUserIdAndStatus(Long userId, FollowStatus status, Pageable pageable) {
        BooleanExpression whereCondition = buildWhereCondition(userId, null, status);
        
        // 1) 페이징 가능한 followingId 조회
        List<Long> followingIds = queryFactory
                .select(follow.followingId)
                .from(follow)
                .where(whereCondition)
                .orderBy(follow.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2) 전체 개수 조회
        Long totalCount = queryFactory
                .select(follow.count())
                .from(follow)
                .where(whereCondition)
                .fetchOne();

        long total = totalCount != null ? totalCount : 0L;

        if (followingIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        // 3) User 조회
        List<User> users = queryFactory
                .selectFrom(user)
                .where(user.id.in(followingIds))
                .fetch();

        // followingIds 순서대로 정렬
        Map<Long, User> userMap = users.stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, u -> u));

        List<User> content = followingIds.stream()
                .map(userMap::get)
                .filter(java.util.Objects::nonNull)
                .toList();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Long countFollowersByUserIdAndStatus(Long userId, FollowStatus status) {
        BooleanExpression whereCondition = buildWhereCondition(null, userId, status);
        
        Long count = queryFactory
                .select(follow.count())
                .from(follow)
                .where(whereCondition)
                .fetchOne();

        return count != null ? count : 0L;
    }

    @Override
    public Long countFollowingByUserIdAndStatus(Long userId, FollowStatus status) {
        BooleanExpression whereCondition = buildWhereCondition(userId, null, status);
        
        Long count = queryFactory
                .select(follow.count())
                .from(follow)
                .where(whereCondition)
                .fetchOne();

        return count != null ? count : 0L;
    }

    /**
     * 동적 where 조건 생성
     * QueryDSL의 null 안전 조합 활용
     */
    private BooleanExpression buildWhereCondition(Long followerId, Long followingId, FollowStatus status) {
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


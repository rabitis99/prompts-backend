package org.example.sharedprompts.domain.follow.repository.user;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.follow.Follow;
import org.example.sharedprompts.domain.follow.FollowStatus;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.dto.follow.response.UserWithFollowInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.example.sharedprompts.domain.follow.QFollow.follow;
import static org.example.sharedprompts.domain.user.QUser.user;

@RequiredArgsConstructor
public class UserFollowRepositoryImpl implements UserFollowRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 종료 상태 (기본적으로 제외)
     */
    private static final List<FollowStatus> EXCLUDED_STATUSES = List.of(
            FollowStatus.REJECTED,
            FollowStatus.CANCELLED,
            FollowStatus.BLOCKED
    );

    /**
     * 팔로우 조회 방향
     */
    private enum FollowDirection {
        FOLLOWERS,
        FOLLOWING
    }

    /**
     * status 조건을 BooleanExpression으로 생성
     */
    private BooleanExpression buildStatusCondition(FollowStatus status) {
        if (status != null) {
            return follow.status.eq(status);
        } else {
            return follow.status.notIn(EXCLUDED_STATUSES);
        }
    }

    @Override
    public Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId) {
        Follow result = queryFactory
                .selectFrom(follow)
                .where(
                        follow.followerId.eq(followerId)
                                .and(follow.followingId.eq(followingId))
                )
                .fetchFirst();
        
        return Optional.ofNullable(result);
    }

    @Override
    public Page<User> findFollowersByUserIdAndStatus(Long userId, FollowStatus status, Pageable pageable) {
        return findUsersByFollowDirection(userId, status, pageable, FollowDirection.FOLLOWERS);
    }

    @Override
    public Page<User> findFollowingByUserIdAndStatus(Long userId, FollowStatus status, Pageable pageable) {
        return findUsersByFollowDirection(userId, status, pageable, FollowDirection.FOLLOWING);
    }

    @Override
    public Long countFollowersByUserIdAndStatus(Long userId, FollowStatus status) {
        return countUsersByFollowDirection(userId, status, FollowDirection.FOLLOWERS);
    }

    @Override
    public Long countFollowingByUserIdAndStatus(Long userId, FollowStatus status) {
        return countUsersByFollowDirection(userId, status, FollowDirection.FOLLOWING);
    }

    /**
     * 팔로워/팔로잉 조회 공통 헬퍼 메서드
     * 
     * @param userId 사용자 ID
     * @param status 팔로우 상태 (null이면 REJECTED, CANCELLED, BLOCKED 제외)
     * @param pageable 페이징 정보
     * @param direction 조회 방향 (FOLLOWERS 또는 FOLLOWING)
     * @return User 페이지
     */
    private Page<User> findUsersByFollowDirection(
            Long userId,
            FollowStatus status,
            Pageable pageable,
            FollowDirection direction
    ) {
        BooleanExpression whereCondition = buildWhereCondition(userId, status, direction);
        
        // Count query
        long total = queryFactory
                .select(user.count())
                .from(user)
                .innerJoin(follow).on(buildJoinCondition(direction))
                .where(whereCondition)
                .fetchOne();

        if (total == 0) {
            return new PageImpl<>(new ArrayList<>(), pageable, 0);
        }

        // Content query
        List<User> content = queryFactory
                .selectFrom(user)
                .innerJoin(follow).on(buildJoinCondition(direction))
                .where(whereCondition)
                .orderBy(follow.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 방향에 따른 조인 조건 생성
     */
    private BooleanExpression buildJoinCondition(FollowDirection direction) {
        return direction == FollowDirection.FOLLOWERS
                ? user.id.eq(follow.followerId)
                : user.id.eq(follow.followingId);
    }

    /**
     * 방향과 status에 따른 WHERE 조건 생성
     */
    private BooleanExpression buildWhereCondition(Long userId, FollowStatus status, FollowDirection direction) {
        BooleanExpression baseCondition = direction == FollowDirection.FOLLOWERS
                ? follow.followingId.eq(userId)
                : follow.followerId.eq(userId);

        BooleanExpression userCondition = user.deletedAt.isNull()
                .and(user.blocked.eq(false));

        BooleanExpression statusCondition = buildStatusCondition(status);

        return baseCondition
                .and(userCondition)
                .and(statusCondition);
    }

    /**
     * 팔로워/팔로잉 카운트 공통 헬퍼 메서드
     * 
     * @param userId 사용자 ID
     * @param status 팔로우 상태 (null이면 REJECTED, CANCELLED, BLOCKED 제외)
     * @param direction 조회 방향 (FOLLOWERS 또는 FOLLOWING)
     * @return 카운트
     */
    private Long countUsersByFollowDirection(
            Long userId,
            FollowStatus status,
            FollowDirection direction
    ) {
        BooleanExpression whereCondition = buildWhereCondition(userId, status, direction);
        
        return queryFactory
                .select(user.count())
                .from(user)
                .innerJoin(follow).on(buildJoinCondition(direction))
                .where(whereCondition)
                .fetchOne();
    }

    @Override
    public Page<UserWithFollowInfo> findFollowersWithFollowInfo(
            Long userId, Long viewerId, FollowStatus status, Pageable pageable) {
        return findUsersWithFollowInfo(userId, viewerId, status, pageable, FollowDirection.FOLLOWERS);
    }

    @Override
    public Page<UserWithFollowInfo> findFollowingWithFollowInfo(
            Long userId, Long viewerId, FollowStatus status, Pageable pageable) {
        return findUsersWithFollowInfo(userId, viewerId, status, pageable, FollowDirection.FOLLOWING);
    }

    /**
     * 팔로워/팔로잉 목록과 양방향 Follow 정보를 함께 조회하는 공통 헬퍼 메서드
     * 
     * @param userId 대상 사용자 ID
     * @param viewerId 조회하는 사용자 ID (양방향 관계 확인용)
     * @param status 필터링할 상태 (null이면 REJECTED, CANCELLED, BLOCKED 제외)
     * @param pageable 페이지 정보
     * @param direction 조회 방향 (FOLLOWERS 또는 FOLLOWING)
     * @return User와 양방향 Follow 정보를 포함한 페이지
     */
    private Page<UserWithFollowInfo> findUsersWithFollowInfo(
            Long userId,
            Long viewerId,
            FollowStatus status,
            Pageable pageable,
            FollowDirection direction
    ) {
        // 먼저 User 목록을 가져옴
        Page<User> userPage = findUsersByFollowDirection(userId, status, pageable, direction);
        
        if (userPage.getContent().isEmpty()) {
            return new PageImpl<>(new ArrayList<>(), pageable, userPage.getTotalElements());
        }
        
        // User 목록에서 ID 추출 및 Map 생성 (중복 조회 방지)
        List<Long> targetIds = userPage.getContent().stream()
                .map(User::getId)
                .toList();
        Map<Long, User> userMap = userPage.getContent().stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        
        // 양방향 Follow 정보만 조회 (User는 이미 조회했으므로 재사용)
        Map<Long, Follow> forwardMap = findForwardFollows(viewerId, targetIds);
        Map<Long, Follow> reverseMap = findReverseFollows(viewerId, targetIds);
        
        // User와 Follow 정보를 매칭하여 결과 생성
        List<UserWithFollowInfo> result = buildUserWithFollowInfoList(
                targetIds, userMap, forwardMap, reverseMap);
        
        return new PageImpl<>(result, pageable, userPage.getTotalElements());
    }

    @Override
    public List<UserWithFollowInfo> findBidirectionalFollowInfo(Long viewerId, List<Long> targetIds) {
        if (targetIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Forward/Reverse Follow 관계 조회
        Map<Long, Follow> forwardMap = findForwardFollows(viewerId, targetIds);
        Map<Long, Follow> reverseMap = findReverseFollows(viewerId, targetIds);
        
        // User 조회
        Map<Long, User> userMap = findUsersByIds(targetIds);
        
        // User와 Follow 정보를 매칭하여 결과 생성
        return buildUserWithFollowInfoList(targetIds, userMap, forwardMap, reverseMap);
    }

    /**
     * Forward Follow 관계 조회 (viewer → target)
     */
    private Map<Long, Follow> findForwardFollows(Long viewerId, List<Long> targetIds) {
        List<Follow> forwardFollows = queryFactory
                .selectFrom(follow)
                .where(
                        follow.followerId.eq(viewerId)
                                .and(follow.followingId.in(targetIds))
                                .and(follow.status.notIn(EXCLUDED_STATUSES))
                )
                .fetch();
        
        return forwardFollows.stream()
                .collect(Collectors.toMap(Follow::getFollowingId, f -> f));
    }

    /**
     * Reverse Follow 관계 조회 (target → viewer)
     */
    private Map<Long, Follow> findReverseFollows(Long viewerId, List<Long> targetIds) {
        List<Follow> reverseFollows = queryFactory
                .selectFrom(follow)
                .where(
                        follow.followerId.in(targetIds)
                                .and(follow.followingId.eq(viewerId))
                                .and(follow.status.notIn(EXCLUDED_STATUSES))
                )
                .fetch();
        
        return reverseFollows.stream()
                .collect(Collectors.toMap(Follow::getFollowerId, f -> f));
    }

    /**
     * User ID 목록으로 User 조회
     */
    private Map<Long, User> findUsersByIds(List<Long> userIds) {
        List<User> users = queryFactory
                .selectFrom(user)
                .where(
                        user.id.in(userIds)
                                .and(user.deletedAt.isNull())
                                .and(user.blocked.eq(false))
                )
                .fetch();
        
        return users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));
    }

    /**
     * User와 Follow 정보를 매칭하여 UserWithFollowInfo 리스트 생성
     */
    private List<UserWithFollowInfo> buildUserWithFollowInfoList(
            List<Long> targetIds,
            Map<Long, User> userMap,
            Map<Long, Follow> forwardMap,
            Map<Long, Follow> reverseMap) {
        
        List<UserWithFollowInfo> result = new ArrayList<>();
        for (Long targetId : targetIds) {
            User user = userMap.get(targetId);
            if (user == null) continue;
            
            Follow forwardFollow = forwardMap.get(targetId);
            Follow reverseFollow = reverseMap.get(targetId);
            
            result.add(UserWithFollowInfo.builder()
                    .user(user)
                    .forwardFollow(forwardFollow)
                    .reverseFollow(reverseFollow)
                    .build());
        }
        
        return result;
    }

    @Override
    public boolean existsBlockedBetween(Long userId1, Long userId2) {
        Long count = queryFactory
                .select(follow.count())
                .from(follow)
                .where(
                        follow.status.eq(FollowStatus.BLOCKED)
                                .and(
                                        follow.followerId.eq(userId1)
                                                .and(follow.followingId.eq(userId2))
                                                .or(
                                                        follow.followerId.eq(userId2)
                                                                .and(follow.followingId.eq(userId1))
                                                )
                                )
                )
                .fetchOne();

        return count != null && count > 0;
    }

    @Override
    public List<Long> findBlockedUserIds(List<Long> followerIds, Long authorId) {
        if (followerIds == null || followerIds.isEmpty() || authorId == null) {
            return List.of();
        }

        // 양방향 BLOCKED 관계 조회
        List<Long> blockedByFollower = findBlockedUserIdsByDirection(
                followerIds, authorId, true);
        List<Long> blockedByFollowing = findBlockedUserIdsByDirection(
                followerIds, authorId, false);

        // 두 결과를 합치고 중복 제거
        return Stream.concat(blockedByFollower.stream(), blockedByFollowing.stream())
                .distinct()
                .toList();
    }

    /**
     * BLOCKED 관계를 특정 방향으로 조회하는 헬퍼 메서드
     *
     * @param followerIds 검사할 팔로워 ID 목록
     * @param authorId 작성자 ID
     * @param isForward true면 followerId를, false면 followingId를 반환
     * @return BLOCKED 관계가 있는 사용자 ID 목록
     */
    private List<Long> findBlockedUserIdsByDirection(
            List<Long> followerIds, Long authorId, boolean isForward) {
        BooleanExpression whereCondition = follow.status.eq(FollowStatus.BLOCKED)
                .and(isForward
                        ? follow.followerId.in(followerIds).and(follow.followingId.eq(authorId))
                        : follow.followingId.in(followerIds).and(follow.followerId.eq(authorId)));

        if (isForward) {
            return queryFactory
                    .select(follow.followerId)
                    .from(follow)
                    .where(whereCondition)
                    .fetch();
        } else {
            return queryFactory
                    .select(follow.followingId)
                    .from(follow)
                    .where(whereCondition)
                    .fetch();
        }
    }

    @Override
    public List<Long> findFollowerIdsByFollowingIdAndStatus(Long followingId, FollowStatus status) {
        return queryFactory
                .select(follow.followerId)
                .from(follow)
                .where(
                        follow.followingId.eq(followingId)
                                .and(follow.status.eq(status))
                )
                .fetch();
    }
}



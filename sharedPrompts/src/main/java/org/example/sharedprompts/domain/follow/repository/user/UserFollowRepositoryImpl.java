package org.example.sharedprompts.domain.follow.repository.user;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
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

@RequiredArgsConstructor
public class UserFollowRepositoryImpl implements UserFollowRepository {

    private final EntityManager entityManager;

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
        FOLLOWERS {
            @Override
            QueryCondition getQueryCondition() {
                return new QueryCondition(
                        "u.id = f.followerId",
                        "f.followingId = :userId"
                );
            }
        },
        FOLLOWING {
            @Override
            QueryCondition getQueryCondition() {
                return new QueryCondition(
                        "u.id = f.followingId",
                        "f.followerId = :userId"
                );
            }
        };

        abstract QueryCondition getQueryCondition();
    }

    /**
     * 쿼리 조건 정보
     */
    private record QueryCondition(
            String joinCondition,
            String whereCondition
    ) {}

    /**
     * status 조건 문자열 생성
     */
    private String buildStatusCondition(FollowStatus status) {
        if (status != null) {
            return " AND f.status = :status";
        } else {
            return " AND f.status NOT IN (:excludedStatuses)";
        }
    }

    /**
     * 쿼리에 status 파라미터를 바인딩
     */
    private void bindStatusParameter(FollowStatus status, Query query) {
        if (status != null) {
            query.setParameter("status", status);
        } else {
            query.setParameter("excludedStatuses", EXCLUDED_STATUSES);
        }
    }

    @Override
    public Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId) {
        String jpql = """
            SELECT f FROM Follow f 
            WHERE f.followerId = :followerId 
            AND f.followingId = :followingId
            """;
        
        List<Follow> results = entityManager.createQuery(jpql, Follow.class)
                .setParameter("followerId", followerId)
                .setParameter("followingId", followingId)
                .getResultList();
        
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
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
        QueryCondition condition = direction.getQueryCondition();
        String statusCondition = buildStatusCondition(status);
        
        String baseQuery = String.format("""
            SELECT u FROM User u
            INNER JOIN Follow f ON %s
            WHERE %s
            AND u.deletedAt IS NULL
            AND u.blocked = false%s
            ORDER BY f.id DESC
            """, condition.joinCondition(), condition.whereCondition(), statusCondition);
        
        String countQuery = String.format("""
            SELECT COUNT(u) FROM User u
            INNER JOIN Follow f ON %s
            WHERE %s
            AND u.deletedAt IS NULL
            AND u.blocked = false%s
            """, condition.joinCondition(), condition.whereCondition(), statusCondition);
        
        var query = entityManager.createQuery(baseQuery, User.class)
                .setParameter("userId", userId);
        var countQ = entityManager.createQuery(countQuery, Long.class)
                .setParameter("userId", userId);
        
        bindStatusParameter(status, query);
        bindStatusParameter(status, countQ);
        
        long total = countQ.getSingleResult();
        
        List<User> content = query
                .setFirstResult(Math.toIntExact(pageable.getOffset()))
                .setMaxResults(pageable.getPageSize())
                .getResultList();
        
        return new PageImpl<>(content, pageable, total);
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
        QueryCondition condition = direction.getQueryCondition();
        String statusCondition = buildStatusCondition(status);
        
        String jpql = String.format("""
            SELECT COUNT(u) FROM User u
            INNER JOIN Follow f ON %s
            WHERE %s
            AND u.deletedAt IS NULL
            AND u.blocked = false%s
            """, condition.joinCondition(), condition.whereCondition(), statusCondition);
        
        var query = entityManager.createQuery(jpql, Long.class)
                .setParameter("userId", userId);
        
        bindStatusParameter(status, query);
        
        return query.getSingleResult();
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
        
        // User 목록에서 ID 추출
        List<Long> targetIds = userPage.getContent().stream()
                .map(User::getId)
                .collect(Collectors.toList());
        
        if (targetIds.isEmpty()) {
            return new PageImpl<>(new ArrayList<>(), pageable, userPage.getTotalElements());
        }
        
        // 양방향 Follow 정보를 배치로 조회
        List<UserWithFollowInfo> followInfoList = findBidirectionalFollowInfo(viewerId, targetIds);
        
        // User와 Follow 정보를 매칭
        Map<Long, UserWithFollowInfo> followInfoMap = followInfoList.stream()
                .collect(Collectors.toMap(
                        info -> info.getUser().getId(),
                        info -> info
                ));
        
        // User 목록에 Follow 정보 매핑
        List<UserWithFollowInfo> result = userPage.getContent().stream()
                .map(user -> {
                    UserWithFollowInfo info = followInfoMap.get(user.getId());
                    if (info != null) {
                        return info;
                    }
                    // Follow 정보가 없는 경우 (양방향 모두 없음)
                    return UserWithFollowInfo.builder()
                            .user(user)
                            .forwardFollow(null)
                            .reverseFollow(null)
                            .build();
                })
                .collect(Collectors.toList());
        
        return new PageImpl<>(result, pageable, userPage.getTotalElements());
    }

    @Override
    public List<UserWithFollowInfo> findBidirectionalFollowInfo(Long viewerId, List<Long> targetIds) {
        if (targetIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // forward: viewer → target
        String forwardJpql = """
            SELECT f FROM Follow f
            WHERE f.followerId = :viewerId
            AND f.followingId IN :targetIds
            AND f.status NOT IN (:excludedStatuses)
            """;
        
        List<Follow> forwardFollows = entityManager.createQuery(forwardJpql, Follow.class)
                .setParameter("viewerId", viewerId)
                .setParameter("targetIds", targetIds)
                .setParameter("excludedStatuses", EXCLUDED_STATUSES)
                .getResultList();
        
        // reverse: target → viewer
        String reverseJpql = """
            SELECT f FROM Follow f
            WHERE f.followerId IN :targetIds
            AND f.followingId = :viewerId
            AND f.status NOT IN (:excludedStatuses)
            """;
        
        List<Follow> reverseFollows = entityManager.createQuery(reverseJpql, Follow.class)
                .setParameter("viewerId", viewerId)
                .setParameter("targetIds", targetIds)
                .setParameter("excludedStatuses", EXCLUDED_STATUSES)
                .getResultList();
        
        // Map으로 변환하여 조회 빠르게
        Map<Long, Follow> forwardMap = forwardFollows.stream()
                .collect(Collectors.toMap(Follow::getFollowingId, f -> f));
        Map<Long, Follow> reverseMap = reverseFollows.stream()
                .collect(Collectors.toMap(Follow::getFollowerId, f -> f));
        
        // User 조회 (targetIds에 해당하는 User들)
        String userJpql = """
            SELECT u FROM User u
            WHERE u.id IN :targetIds
            AND u.deletedAt IS NULL
            AND u.blocked = false
            """;
        
        List<User> users = entityManager.createQuery(userJpql, User.class)
                .setParameter("targetIds", targetIds)
                .getResultList();
        
        // User와 Follow 정보를 매칭하여 결과 생성
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        
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
}



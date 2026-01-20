package org.example.sharedprompts.domain.follow.repository.user;

import jakarta.persistence.EntityManager;
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
        String baseQuery = """
            SELECT u FROM User u
            INNER JOIN Follow f ON u.id = f.followerId
            WHERE f.followingId = :userId
            AND u.deletedAt IS NULL
            AND u.blocked = false
            """;
        
        String countQuery = """
            SELECT COUNT(u) FROM User u
            INNER JOIN Follow f ON u.id = f.followerId
            WHERE f.followingId = :userId
            AND u.deletedAt IS NULL
            AND u.blocked = false
            """;
        
        if (status != null) {
            // 명시적인 status가 주어지면 해당 상태만 필터링
            baseQuery += " AND f.status = :status";
            countQuery += " AND f.status = :status";
        } else {
            // status가 null이면 종료 상태(REJECTED, CANCELLED)는 기본적으로 제외
            baseQuery += " AND f.status NOT IN ('REJECTED', 'CANCELLED')";
            countQuery += " AND f.status NOT IN ('REJECTED', 'CANCELLED')";
        }
        
        baseQuery += " ORDER BY f.id DESC";
        
        var query = entityManager.createQuery(baseQuery, User.class)
                .setParameter("userId", userId);
        var countQ = entityManager.createQuery(countQuery, Long.class)
                .setParameter("userId", userId);
        
        if (status != null) {
            query.setParameter("status", status);
            countQ.setParameter("status", status);
        }
        
        long total = countQ.getSingleResult();
        
        List<User> content = query
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();
        
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<User> findFollowingByUserIdAndStatus(Long userId, FollowStatus status, Pageable pageable) {
        String baseQuery = """
            SELECT u FROM User u
            INNER JOIN Follow f ON u.id = f.followingId
            WHERE f.followerId = :userId
            AND u.deletedAt IS NULL
            AND u.blocked = false
            """;
        
        String countQuery = """
            SELECT COUNT(u) FROM User u
            INNER JOIN Follow f ON u.id = f.followingId
            WHERE f.followerId = :userId
            AND u.deletedAt IS NULL
            AND u.blocked = false
            """;
        
        if (status != null) {
            // 명시적인 status가 주어지면 해당 상태만 필터링
            baseQuery += " AND f.status = :status";
            countQuery += " AND f.status = :status";
        } else {
            // status가 null이면 종료 상태(REJECTED, CANCELLED)는 기본적으로 제외
            baseQuery += " AND f.status NOT IN ('REJECTED', 'CANCELLED')";
            countQuery += " AND f.status NOT IN ('REJECTED', 'CANCELLED')";
        }
        
        baseQuery += " ORDER BY f.id DESC";
        
        var query = entityManager.createQuery(baseQuery, User.class)
                .setParameter("userId", userId);
        var countQ = entityManager.createQuery(countQuery, Long.class)
                .setParameter("userId", userId);
        
        if (status != null) {
            query.setParameter("status", status);
            countQ.setParameter("status", status);
        }
        
        long total = countQ.getSingleResult();
        
        List<User> content = query
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();
        
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Long countFollowersByUserIdAndStatus(Long userId, FollowStatus status) {
        String jpql = """
            SELECT COUNT(u) FROM User u
            INNER JOIN Follow f ON u.id = f.followerId
            WHERE f.followingId = :userId
            AND u.deletedAt IS NULL
            AND u.blocked = false
            """;
        
        if (status != null) {
            // 명시적인 status가 주어지면 해당 상태만 카운트
            jpql += " AND f.status = :status";
        } else {
            // status가 null이면 종료 상태(REJECTED, CANCELLED)는 기본적으로 제외
            jpql += " AND f.status NOT IN ('REJECTED', 'CANCELLED')";
        }
        
        var query = entityManager.createQuery(jpql, Long.class)
                .setParameter("userId", userId);
        
        if (status != null) {
            query.setParameter("status", status);
        }
        
        return query.getSingleResult();
    }

    @Override
    public Long countFollowingByUserIdAndStatus(Long userId, FollowStatus status) {
        String jpql = """
            SELECT COUNT(u) FROM User u
            INNER JOIN Follow f ON u.id = f.followingId
            WHERE f.followerId = :userId
            AND u.deletedAt IS NULL
            AND u.blocked = false
            """;
        
        if (status != null) {
            // 명시적인 status가 주어지면 해당 상태만 카운트
            jpql += " AND f.status = :status";
        } else {
            // status가 null이면 종료 상태(REJECTED, CANCELLED)는 기본적으로 제외
            jpql += " AND f.status NOT IN ('REJECTED', 'CANCELLED')";
        }
        
        var query = entityManager.createQuery(jpql, Long.class)
                .setParameter("userId", userId);
        
        if (status != null) {
            query.setParameter("status", status);
        }
        
        return query.getSingleResult();
    }

    @Override
    public Page<UserWithFollowInfo> findFollowersWithFollowInfo(
            Long userId, Long viewerId, FollowStatus status, Pageable pageable) {
        // 먼저 User 목록을 가져옴
        Page<User> userPage = findFollowersByUserIdAndStatus(userId, status, pageable);
        
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
    public Page<UserWithFollowInfo> findFollowingWithFollowInfo(
            Long userId, Long viewerId, FollowStatus status, Pageable pageable) {
        // 먼저 User 목록을 가져옴
        Page<User> userPage = findFollowingByUserIdAndStatus(userId, status, pageable);
        
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
            """;
        
        List<Follow> forwardFollows = entityManager.createQuery(forwardJpql, Follow.class)
                .setParameter("viewerId", viewerId)
                .setParameter("targetIds", targetIds)
                .getResultList();
        
        // reverse: target → viewer
        String reverseJpql = """
            SELECT f FROM Follow f
            WHERE f.followerId IN :targetIds
            AND f.followingId = :viewerId
            """;
        
        List<Follow> reverseFollows = entityManager.createQuery(reverseJpql, Follow.class)
                .setParameter("viewerId", viewerId)
                .setParameter("targetIds", targetIds)
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



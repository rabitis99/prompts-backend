package org.example.sharedprompts.domain.follow.repository.admin;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.follow.FollowStatus;
import org.example.sharedprompts.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Admin 전용 Follow 조회 Repository 구현.
 *
 * - status는 필수 (Controller에서 보장)
 * - deleted/blocked 유저 포함 조회 가능 (관리 목적)
 */
@Repository
@RequiredArgsConstructor
public class AdminFollowRepositoryImpl implements AdminFollowRepository {

    private final EntityManager entityManager;

    @Override
    public Page<User> findUsersByCondition(
            Long followerId,
            Long followingId,
            FollowStatus status,
            Pageable pageable
    ) {
        String baseQuery;
        String countQuery;
        
        if (followerId != null && followingId != null) {
            // 둘 다 있으면 특정 관계의 User 조회 (실제로는 의미가 없지만 요구사항에 따라)
            baseQuery = """
                SELECT u FROM User u
                INNER JOIN Follow f ON u.id = f.followingId
                WHERE f.followerId = :followerId
                AND f.followingId = :followingId
                AND f.status = :status
                ORDER BY f.id DESC
                """;
            countQuery = """
                SELECT COUNT(u) FROM User u
                INNER JOIN Follow f ON u.id = f.followingId
                WHERE f.followerId = :followerId
                AND f.followingId = :followingId
                AND f.status = :status
                """;
        } else if (followerId != null) {
            // followerId가 있으면 해당 사용자가 팔로우하는 사람들 조회
            baseQuery = """
                SELECT u FROM User u
                INNER JOIN Follow f ON u.id = f.followingId
                WHERE f.followerId = :followerId
                AND f.status = :status
                ORDER BY f.id DESC
                """;
            countQuery = """
                SELECT COUNT(u) FROM User u
                INNER JOIN Follow f ON u.id = f.followingId
                WHERE f.followerId = :followerId
                AND f.status = :status
                """;
        } else if (followingId != null) {
            // followingId가 있으면 해당 사용자를 팔로우하는 사람들 조회
            baseQuery = """
                SELECT u FROM User u
                INNER JOIN Follow f ON u.id = f.followerId
                WHERE f.followingId = :followingId
                AND f.status = :status
                ORDER BY f.id DESC
                """;
            countQuery = """
                SELECT COUNT(u) FROM User u
                INNER JOIN Follow f ON u.id = f.followerId
                WHERE f.followingId = :followingId
                AND f.status = :status
                """;
        } else {
            // 둘 다 null이면 status만으로 조회 (followerId 기준)
            baseQuery = """
                SELECT DISTINCT u FROM User u
                INNER JOIN Follow f ON u.id = f.followingId
                WHERE f.status = :status
                ORDER BY u.id DESC
                """;
            countQuery = """
                SELECT COUNT(DISTINCT u) FROM User u
                INNER JOIN Follow f ON u.id = f.followingId
                WHERE f.status = :status
                """;
        }
        
        var query = entityManager.createQuery(baseQuery, User.class)
                .setParameter("status", status);
        var countQ = entityManager.createQuery(countQuery, Long.class)
                .setParameter("status", status);
        
        if (followerId != null) {
            query.setParameter("followerId", followerId);
            countQ.setParameter("followerId", followerId);
        }
        if (followingId != null) {
            query.setParameter("followingId", followingId);
            countQ.setParameter("followingId", followingId);
        }
        
        long total = countQ.getSingleResult();
        
        List<User> content = query
                .setFirstResult(Math.toIntExact(pageable.getOffset()))
                .setMaxResults(pageable.getPageSize())
                .getResultList();
        
        return new PageImpl<>(content, pageable, total);
    }
}



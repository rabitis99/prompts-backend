package org.example.sharedprompts.domain.user.repository;

import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    
    @Query("SELECT u FROM User u WHERE u.provider = :provider AND u.providerId = :providerId AND u.deletedAt IS NULL")
    Optional<User> findByProviderAndProviderId(@Param("provider") Provider provider, @Param("providerId") String providerId);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.provider = :provider AND u.providerId = :providerId AND u.deletedAt IS NULL")
    boolean existsByProviderAndProviderId(@Param("provider") Provider provider, @Param("providerId") String providerId);

    /**
     * 활성 사용자 수 조회 (최근 30일 내 활동)
     */
    @Query("SELECT COUNT(DISTINCT u.id) FROM User u WHERE u.updatedAt >= :since AND u.deletedAt IS NULL")
    Long countActiveUsersSince(@Param("since") LocalDateTime since);

    /**
     * 특정 기간 내 신규 가입자 수 조회
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startDate AND u.createdAt < :endDate AND u.deletedAt IS NULL")
    Long countNewUsersBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * 일별 신규 가입자 수 조회 (최근 30일 - 오늘 포함)
     */
    @Query(value = """
        SELECT DATE(created_at) as date, COUNT(*) as count
        FROM users
        WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 29 DAY)
        AND deleted_at IS NULL
        GROUP BY DATE(created_at)
        ORDER BY date ASC
        """, nativeQuery = true)
    List<DailyUserCountProjection> countDailyNewUsersLast30Days();

    /**
     * ID로 사용자 조회 (삭제되지 않은 사용자만)
     */
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.deletedAt IS NULL")
    Optional<User> findByIdAndDeletedAtIsNull(@Param("id") Long id);

    /**
     * 관리자 계정 수 조회 (삭제되지 않은 계정만)
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'ROLE_ADMIN' AND u.deletedAt IS NULL")
    long countActiveAdmins();

    /**
     * 닉네임 또는 이메일로 사용자 검색 (관리자용 - 삭제된 사용자 포함)
     */
    @Query("""
        SELECT u FROM User u 
        WHERE (:keyword IS NULL OR :keyword = '' OR 
               LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')) OR 
               LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
        ORDER BY u.createdAt DESC
        """)
    org.springframework.data.domain.Page<User> searchUsers(
            @Param("keyword") String keyword,
            org.springframework.data.domain.Pageable pageable
    );
}

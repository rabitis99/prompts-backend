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
    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);

    boolean existsByProviderAndProviderId(Provider provider, String providerId);

    /**
     * 활성 사용자 수 조회 (최근 30일 내 활동)
     */
    @Query("SELECT COUNT(DISTINCT u.id) FROM User u WHERE u.updatedAt >= :since")
    Long countActiveUsersSince(@Param("since") LocalDateTime since);

    /**
     * 특정 기간 내 신규 가입자 수 조회
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startDate AND u.createdAt < :endDate")
    Long countNewUsersBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * 일별 신규 가입자 수 조회 (최근 30일)
     */
    @Query(value = """
        SELECT DATE(created_at) as date, COUNT(*) as count
        FROM users
        WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
        GROUP BY DATE(created_at)
        ORDER BY date ASC
        """, nativeQuery = true)
    List<Object[]> countDailyNewUsersLast30Days();
}

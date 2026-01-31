package org.example.sharedprompts.domain.payment.repository.userTier;

import org.example.sharedprompts.domain.payment.UserTierHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 사용자 티어 변경 이력 Repository
 */
public interface UserTierHistoryRepository extends JpaRepository<UserTierHistory, Long>, CustomUserTierHistoryRepository {

    /**
     * 사용자의 티어 변경 이력 조회
     */
    List<UserTierHistory> findByUser_IdOrderByCreatedAtDesc(Long userId);
}


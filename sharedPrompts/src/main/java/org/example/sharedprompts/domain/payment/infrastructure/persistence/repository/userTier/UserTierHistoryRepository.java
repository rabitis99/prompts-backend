package org.example.sharedprompts.domain.payment.infrastructure.persistence.repository.userTier;

import org.example.sharedprompts.domain.payment.domain.entity.UserTierHistory;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 사용자 티어 변경 이력 Repository
 */
public interface UserTierHistoryRepository extends JpaRepository<UserTierHistory, Long>, CustomUserTierHistoryRepository {
}


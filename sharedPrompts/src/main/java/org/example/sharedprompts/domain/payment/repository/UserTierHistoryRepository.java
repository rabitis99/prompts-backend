package org.example.sharedprompts.domain.payment.repository;

import org.example.sharedprompts.domain.payment.UserTierHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 사용자 티어 변경 이력 Repository
 */
public interface UserTierHistoryRepository extends JpaRepository<UserTierHistory, Long> {

    /**
 * Retrieve a user's tier change history ordered by creation time, newest first.
 *
 * @param userId the identifier of the user whose tier history to retrieve
 * @return a list of UserTierHistory records for the given user, ordered by createdAt in descending order
 */
    List<UserTierHistory> findByUser_IdOrderByCreatedAtDesc(Long userId);
}

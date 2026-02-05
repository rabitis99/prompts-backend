package org.example.sharedprompts.domain.payment.infrastructure.persistence.repository.userTier;

import org.example.sharedprompts.domain.payment.domain.entity.UserTierHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomUserTierHistoryRepository {

    /**
     * 2-step 페이징 + fetchJoin
     * 티어 변경 이력 조회 시 N+1 문제 해결
     */
    Page<UserTierHistory> findByUserIdWithFetchJoin(Long userId, Pageable pageable);
}

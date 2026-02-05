package org.example.sharedprompts.domain.payment.infrastructure.persistence.repository.cashback;

import org.example.sharedprompts.domain.payment.domain.entity.Cashback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomCashbackRepository {
    /**
     * 사용자의 캐시백 내역 조회 (페이징 + fetchJoin 최적화)
     */
    Page<Cashback> findByUserIdWithFetchJoin(Long userId, Pageable pageable);

    /**
     * 미지급 캐시백 목록 조회 (페이징 + fetchJoin 최적화)
     */
    Page<Cashback> findUnpaidByUserIdWithFetchJoin(Long userId, Pageable pageable);

    /**
     * 전체 미지급 캐시백 목록 조회 (관리자용 - 페이징 + fetchJoin 최적화)
     */
    Page<Cashback> findAllUnpaidWithFetchJoin(Pageable pageable);
}

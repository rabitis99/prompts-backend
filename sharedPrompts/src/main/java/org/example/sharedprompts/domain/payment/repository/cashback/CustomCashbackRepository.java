package org.example.sharedprompts.domain.payment.repository;

import org.example.sharedprompts.domain.payment.Cashback;
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
}

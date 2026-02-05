package org.example.sharedprompts.domain.payment.infrastructure.persistence.repository.payment;

import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomPaymentRepository {

    /**
     * 2-step 페이징 + fetchJoin
     * 결제 내역 조회 시 N+1 문제 해결
     */
    Page<Payment> findByUserIdWithFetchJoin(Long userId, Pageable pageable);

    /**
     * 전체 결제 내역 조회 (관리자용)
     * 2-step 페이징 + fetchJoin
     */
    Page<Payment> findAllWithFetchJoin(Pageable pageable);
}

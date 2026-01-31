package org.example.sharedprompts.domain.payment.repository.point;

import org.example.sharedprompts.domain.payment.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomPointRepository {
    /**
     * 사용자의 포인트 내역 조회 (페이징 + fetchJoin 최적화)
     */
    Page<Point> findByUserIdWithFetchJoin(Long userId, Pageable pageable);

    /**
     * 결제와 연관된 포인트 조회 (페이징 + fetchJoin 최적화)
     */
    Page<Point> findByPaymentIdAndUserIdWithFetchJoin(Long paymentId, Long userId, Pageable pageable);

    /**
     * 결제와 연관된 포인트 조회 (관리자용 - userId 필터 없음)
     */
    Page<Point> findByPaymentIdWithFetchJoin(Long paymentId, Pageable pageable);
}

package org.example.sharedprompts.domain.payment.service.cashback.validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.domain.entity.Cashback;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.CashbackJpaAdapter;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

/**
 * 캐시백 검증 서비스
 * 
 * <p>단일 책임: 캐시백 관련 모든 검증만 담당
 * - 중복 적립 방지 검증
 * - 소유권 검증
 * - 미지급 상태 검증
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CashbackValidationService {

    private final CashbackJpaAdapter cashbackJpaAdapter;

    /**
     * 중복 적립 방지 검증
     * 
     * @return 이미 적립된 경우 true, 아니면 false
     */
    public boolean isAlreadyAccumulated(Long paymentId) {
        boolean exists = cashbackJpaAdapter.findByPaymentId(paymentId).isPresent();
        if (exists) {
            log.warn("이미 캐시백이 적립된 결제입니다: paymentId={}", paymentId);
        }
        return exists;
    }

    /**
     * 소유권 검증
     */
    public void validateOwnership(Cashback cashback, Long userId) {
        if (!cashback.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "캐시백 지급 권한이 없습니다.");
        }
    }

    /**
     * 미지급 상태 검증
     */
    public void validateNotPaid(Cashback cashback) {
        if (cashback.isPaid()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "이미 지급된 캐시백입니다.");
        }
    }
}


package org.example.sharedprompts.domain.payment.service.rule;

import org.example.sharedprompts.domain.payment.enums.UserTier;

/**
 * 결제 제한 규칙 인터페이스
 *
 * <p><strong>책임:</strong>
 * <ul>
 *   <li>일일 결제 제한 검증</li>
 *   <li>티어별 제한 정책 관리</li>
 * </ul>
 *
 * <p><strong>사용 목적:</strong>
 * 문서(PAYMENT_DOMAIN_ARCHITECTURE_ANALYSIS.md)의 개선 제안에 따라
 * 비즈니스 규칙을 도메인 서비스로 명확히 분리하기 위해:
 * <ul>
 *   <li>일일 결제 제한 규칙을 별도 Rule로 분리</li>
 *   <li>정책 변경 시 ValidationService 수정 불필요</li>
 *   <li>테스트 시 Mock으로 교체 가능</li>
 * </ul>
 *
 * @see org.example.sharedprompts.domain.payment.service.validation.PaymentValidationService
 */
public interface PaymentLimitRule {

    /**
     * 일일 결제 제한을 검증합니다.
     *
     * @param userId 사용자 ID
     * @param tier 사용자 티어
     * @param todayPaymentCount 오늘 성공한 결제 횟수
     * @throws org.example.sharedprompts.global.exception.ApiException 제한 초과 시
     */
    void validateDailyLimit(Long userId, UserTier tier, long todayPaymentCount);
}


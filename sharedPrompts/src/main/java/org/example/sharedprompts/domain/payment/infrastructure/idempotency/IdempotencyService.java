package org.example.sharedprompts.domain.payment.infrastructure.idempotency;

import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.application.command.PaymentExecutionService;

/**
 * 멱등성 키 생성 서비스 인터페이스
 *
 * <p><strong>책임:</strong>
 * <ul>
 *   <li>멱등성 키 생성 전략 관리</li>
 *   <li>결제, 취소, 환불 등 다양한 액션에 대한 멱등성 키 생성</li>
 *   <li>부분 환불 지원 (환불 누적 금액 기반)</li>
 * </ul>
 *
 * <p><strong>사용 목적:</strong>
 * 문서(PAYMENT_DOMAIN_ARCHITECTURE_ANALYSIS.md)의 개선 제안에 따라
 * 멱등성 키 생성 로직을 ExecutionService에서 분리하여:
 * <ul>
 *   <li>멱등성 전략 변경 시 ExecutionService 수정 불필요</li>
 *   <li>멱등성 키 생성은 도메인 책임, Provider 호출은 인프라 책임으로 명확히 분리</li>
 *   <li>테스트 시 Mock으로 교체 가능</li>
 * </ul>
 *
 * <p><strong>멱등성 키 형식:</strong>
 * <ul>
 *   <li>결제: {paymentMethod}:{paymentId}</li>
 *   <li>취소: {paymentMethod}:{paymentId}:cancel</li>
 *   <li>환불: {paymentMethod}:{paymentId}:refund:{refundedAmount}</li>
 * </ul>
 *
 * @see PaymentExecutionService
 */
public interface IdempotencyService {

    /**
     * 결제에 대한 멱등성 키를 생성합니다.
     *
     * @param payment Payment 엔티티
     * @return 멱등성 키
     */
    String generateForPayment(Payment payment);

    /**
     * 취소에 대한 멱등성 키를 생성합니다.
     *
     * @param payment Payment 엔티티
     * @return 멱등성 키
     */
    String generateForCancel(Payment payment);

    /**
     * 환불에 대한 멱등성 키를 생성합니다.
     * @param payment Payment 엔티티
     * @return 환불 멱등성 키
     */
    String generateForRefund(Payment payment);
}


package org.example.sharedprompts.domain.payment.service.execution;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.model.CancelResult;
import org.example.sharedprompts.domain.payment.model.PaymentResult;
import org.example.sharedprompts.domain.payment.model.RefundResult;
import org.example.sharedprompts.domain.payment.provider.PaymentProvider;
import org.example.sharedprompts.domain.payment.provider.PaymentProviderFactory;
import org.example.sharedprompts.domain.payment.repository.payment.PaymentRepository;
import org.example.sharedprompts.domain.payment.validator.PaymentValidator;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

/**
 * 결제 실행 서비스
 *
 * <p>단일 책임: 외부 Provider 호출 및 결제 실행만 담당
 * - Provider 호출 및 PaymentResult 변환
 * - PaymentValidator를 통한 검증
 * - Payment 도메인 메서드를 통한 상태 변경
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentExecutionService {

    private final PaymentProviderFactory providerFactory;
    private final PaymentValidator paymentValidator;
    private final PaymentRepository paymentRepository;

    // Self-injection for calling @Transactional(propagation = REQUIRES_NEW) methods
    @Autowired
    @Lazy
    private PaymentExecutionService self;
    
    /**
     * 결제 실행
     *
     * @param payment Payment 엔티티
     * @param actualAmount 실제 결제 금액 (포인트 사용 후)
     * @return 저장된 Payment 엔티티
     */
    @Transactional
    public Payment executePayment(Payment payment, BigDecimal actualAmount) {
        return executePayment(payment, actualAmount, Collections.emptyMap());
    }

    /**
     * 결제 실행 (추가 파라미터 포함)
     *
     * @param payment Payment 엔티티
     * @param actualAmount 실제 결제 금액 (포인트 사용 후)
     * @param additionalParams 결제사별 추가 파라미터 (KakaoPay: pgToken 등)
     * @return 저장된 Payment 엔티티
     */
    @Transactional
    public Payment executePayment(Payment payment, BigDecimal actualAmount, Map<String, String> additionalParams) {
        // 이미 SUCCESS 상태면 외부 API 재호출 금지 (멱등성)
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Payment가 이미 완료 상태: paymentId={}, externalPaymentId={}",
                    payment.getId(), payment.getExternalPaymentId());
            return payment;
        }

        // 멱등성 키 생성 및 별도 트랜잭션으로 저장
        // 외부 API 호출 전에 멱등성 키를 커밋하여 API 호출 실패 시에도 유지
        String idempotencyKey = generateIdempotencyKey(payment);
        self.saveIdempotencyKeyInNewTransaction(payment.getId(), idempotencyKey);
        payment.updateIdempotencyKey(idempotencyKey);

        // Provider 선택 및 결제 승인 호출
        PaymentProvider provider = providerFactory.getProvider(payment.getPaymentMethod());

        // externalPaymentId(paymentKey) 검증
        // 일반 결제 승인: 클라이언트에서 받은 paymentKey가 필요
        // 재시도: 이전 시도에서 받은 paymentKey가 있으면 사용, 없으면 재시도 불가
        if (payment.getExternalPaymentId() == null || payment.getExternalPaymentId().isEmpty()) {
            // 재시도 중인 경우 (retryCount > 0)에는 재시도 불가능
            if (payment.getRetryCount() > 0) {
                throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, 
                        "재시도할 수 없습니다. paymentKey가 없습니다. 새로운 결제를 요청해주세요.");
            }
            // 첫 시도인 경우
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, 
                    "결제 승인을 위해서는 paymentKey가 필요합니다. /payments/confirm 엔드포인트를 사용해주세요.");
        }

        PaymentResult result = provider.confirmPayment(
                payment.getExternalPaymentId(),
                String.valueOf(payment.getId()),
                actualAmount,
                payment.getCurrency(),
                idempotencyKey,
                String.valueOf(payment.getUser().getId()),
                additionalParams != null ? additionalParams : Collections.emptyMap()
        );

        // 외부 결제 ID 먼저 저장 (검증 실패해도 추적 가능하도록)
        if (result.getExternalPaymentId() != null) {
            payment.updateExternalPaymentId(result.getExternalPaymentId());
        }

        try {
            // PaymentResult 검증
            paymentValidator.validatePaymentResult(payment, result, actualAmount);

            // 도메인 메서드를 통한 상태 변경
            if (result.isSuccess()) {
                payment.markSuccess(result.getExternalPaymentId());
            } else {
                payment.markFailed(result.getFailureReason() != null ? result.getFailureReason() : "결제 승인 실패");
            }
        } catch (ApiException e) {
            // 검증 실패 시에도 결제 결과를 기록 (불일치 상태로 표시)
            log.error("결제 검증 실패 - 외부 결제는 완료되었으나 검증 불일치: paymentId={}, externalPaymentId={}, error={}",
                    payment.getId(), result.getExternalPaymentId(), e.getMessage());
            payment.markFailed("검증 실패: " + e.getMessage());
            paymentRepository.save(payment);
            throw e;
        }

        return paymentRepository.save(payment);
    }
    
    /**
     * 결제 취소 실행
     *
     * @return 저장된 Payment 엔티티
     * @throws ApiException 취소 실패 시
     */
    @Transactional
    public Payment executeCancel(Payment payment, String reason) {
        if (payment.getExternalPaymentId() == null) {
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, "외부 결제 ID가 없습니다.");
        }

        String idempotencyKey = generateIdempotencyKey(payment, "cancel");
        PaymentProvider provider = providerFactory.getProvider(payment.getPaymentMethod());

        CancelResult result = provider.cancelPayment(payment.getExternalPaymentId(), reason, idempotencyKey);

        if (!result.isSuccess()) {
            throw new ApiException(ErrorCode.PAYMENT_CANCEL_FAILED, "결제 취소 실패");
        }

        payment.markCanceled();
        
        // 카카오페이 취소 시 원본 금액 및 면세 금액 저장 (이후 환불 시 재사용)
        if (result.getOriginalAmount() != null && result.getTaxFreeAmount() != null) {
            payment.updateOriginalAmounts(result.getOriginalAmount(), result.getTaxFreeAmount());
        }
        
        return paymentRepository.save(payment);
    }
    
    /**
     * 결제 환불 실행
     *
     * <p><strong>부분 환불 멱등성 (2026-02-04 개선):</strong>
     * 부분 환불 시 현재 환불 누적 금액(refundedAmount)을 멱등성 키에 포함하여
     * 동일 Payment에 대한 여러 번의 부분 환불 요청을 구분합니다.
     *
     * <p><strong>동시성 보호 (2026-02-04 개선):</strong>
     * 멱등성 키를 외부 API 호출 전에 별도 트랜잭션(REQUIRES_NEW)으로 먼저 저장하여
     * 동시 요청 시 동일한 키가 생성되는 경쟁 조건을 방지합니다.
     * executePayment()와 동일한 패턴을 따릅니다.
     *
     * @return 저장된 Payment 엔티티
     * @throws ApiException 환불 실패 시
     */
    @Transactional
    public Payment executeRefund(Payment payment, BigDecimal refundAmount, String reason) {
        if (payment.getExternalPaymentId() == null) {
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, "외부 결제 ID가 없습니다.");
        }

        // 부분 환불 구분을 위해 환불 전용 멱등성 키 생성
        // 동시성 보호: 외부 API 호출 전에 멱등성 키를 별도 트랜잭션으로 먼저 저장
        // 이를 통해 동시 요청 시 동일한 refundedAmount를 읽어 같은 키가 생성되는 경쟁 조건을 방지
        String idempotencyKey = generateRefundIdempotencyKey(payment);
        self.saveIdempotencyKeyInNewTransaction(payment.getId(), idempotencyKey);
        payment.updateIdempotencyKey(idempotencyKey);

        PaymentProvider provider = providerFactory.getProvider(payment.getPaymentMethod());

        RefundResult result = provider.refundPayment(payment.getExternalPaymentId(), refundAmount, reason, idempotencyKey);

        if (!result.isSuccess()) {
            throw new ApiException(ErrorCode.PAYMENT_REFUND_FAILED, "결제 환불 실패");
        }

        // 실제 환불된 금액으로 업데이트 (결제사 응답 기준)
        BigDecimal actualRefundedAmount = result.getRefundedAmount() != null ? result.getRefundedAmount() : refundAmount;
        payment.refund(actualRefundedAmount);
        return paymentRepository.save(payment);
    }
    
    /**
     * 멱등성 키 생성
     *
     * <p>결정론적 키 생성: 동일한 Payment에 대해 항상 같은 키 반환
     * - 기존에 저장된 idempotencyKey가 있으면 재사용
     * - 없으면 paymentMethod:paymentId 형식으로 생성
     */
    private String generateIdempotencyKey(Payment payment) {
        if (payment.getIdempotencyKey() != null) {
            return payment.getIdempotencyKey();
        }
        return String.format("%s:%s",
                payment.getPaymentMethod().name(),
                payment.getId());
    }

    /**
     * 멱등성 키 생성 (액션 포함)
     *
     * <p>취소/환불 등 특정 액션에 대한 결정론적 키 생성
     */
    private String generateIdempotencyKey(Payment payment, String action) {
        return String.format("%s:%s:%s",
                payment.getPaymentMethod().name(),
                payment.getId(),
                action);
    }

    /**
     * 멱등성 키 생성 (환불 전용 - 부분 환불 구분)
     *
     * <p><strong>부분 환불 지원 (2026-02-04 개선):</strong>
     * 동일 Payment에 대해 여러 번의 부분 환불을 구분하기 위해
     * 현재까지의 환불 누적 금액(refundedAmount)을 키에 포함합니다.
     *
     * <p><strong>예시:</strong>
     * <ul>
     *   <li>첫 번째 부분 환불: TOSS:123:refund:0</li>
     *   <li>두 번째 부분 환불: TOSS:123:refund:5000</li>
     *   <li>세 번째 부분 환불: TOSS:123:refund:10000</li>
     * </ul>
     *
     * <p><strong>주의:</strong>
     * 결제사에서 멱등성을 지원하지 않는 경우 (예: 카카오페이),
     * 이 키는 애플리케이션 레벨에서의 중복 방지 용도로만 사용됩니다.
     *
     * @param payment Payment 엔티티
     * @return 환불 멱등성 키
     */
    private String generateRefundIdempotencyKey(Payment payment) {
        // refundedAmount를 포함하여 각 부분 환불 요청을 구분
        // refundedAmount가 같은 상태에서 재시도하면 같은 키가 생성되어 멱등성 보장
        // null 방어: DB에서 로드 시 null일 수 있으므로 기본값 사용
        BigDecimal refundedAmount = payment.getRefundedAmount() != null
                ? payment.getRefundedAmount()
                : BigDecimal.ZERO;
        return String.format("%s:%s:refund:%s",
                payment.getPaymentMethod().name(),
                payment.getId(),
                refundedAmount.stripTrailingZeros().toPlainString());
    }

    /**
     * 멱등성 키를 별도 트랜잭션으로 저장
     *
     * <p>외부 API 호출 전에 멱등성 키를 먼저 커밋하여
     * API 호출 실패로 메인 트랜잭션이 롤백되어도 멱등성 키는 유지되도록 보장
     *
     * <p>REQUIRES_NEW 전파 속성으로 새로운 트랜잭션에서 실행되며
     * 즉시 커밋되어 외부 API 호출 실패와 무관하게 DB에 저장됨
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveIdempotencyKeyInNewTransaction(Long paymentId, String idempotencyKey) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
        payment.updateIdempotencyKey(idempotencyKey);
        paymentRepository.save(payment);
        log.debug("멱등성 키 저장 완료 (별도 트랜잭션): paymentId={}, idempotencyKey={}", paymentId, idempotencyKey);
    }
}


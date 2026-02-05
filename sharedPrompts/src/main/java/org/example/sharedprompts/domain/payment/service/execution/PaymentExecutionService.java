package org.example.sharedprompts.domain.payment.service.execution;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.model.CancelResult;
import org.example.sharedprompts.domain.payment.model.PaymentResult;
import org.example.sharedprompts.domain.payment.model.RefundResult;
import org.example.sharedprompts.domain.payment.provider.PaymentProvider;
import org.example.sharedprompts.domain.payment.provider.PaymentProviderFactory;
import org.example.sharedprompts.domain.payment.provider.toss.exception.DuplicateOrderIdException;
import org.example.sharedprompts.domain.payment.repository.payment.PaymentRepository;
import org.example.sharedprompts.domain.payment.service.idempotency.IdempotencyService;
import org.example.sharedprompts.domain.payment.service.retry.RetryStrategy;
import org.example.sharedprompts.domain.payment.validator.PaymentValidator;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
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
    private final IdempotencyService idempotencyService;
    private final RetryStrategy immediateRetryStrategy;

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
     * @param payment Payment 엔티티
     * @param actualAmount 실제 결제 금액 (포인트 사용 후)
     * @param additionalParams 결제사별 추가 파라미터 (KakaoPay: pgToken 등)
     * @return 저장된 Payment 엔티티
     */
    public Payment executePayment(Payment payment, BigDecimal actualAmount, Map<String, String> additionalParams) {
        // 재시도 로직은 트랜잭션 외부에서 처리
        return executePaymentWithImmediateRetry(payment, actualAmount, additionalParams, 0);
    }

    /**
     * 결제 실행 (즉시 재시도 포함)
     * 
     * <p>재시도 로직은 트랜잭션 외부에서 처리하여 DB 연결 유지 문제를 방지합니다.
     *
     * @param payment Payment 엔티티
     * @param actualAmount 실제 결제 금액 (포인트 사용 후)
     * @param additionalParams 결제사별 추가 파라미터
     * @param immediateRetryCount 현재 즉시 재시도 횟수
     * @return 저장된 Payment 엔티티
     */
    private Payment executePaymentWithImmediateRetry(
            Payment payment, 
            BigDecimal actualAmount, 
            Map<String, String> additionalParams,
            int immediateRetryCount
    ) {
        try {
            // 트랜잭션 내부에서 실제 결제 실행
            return self.executePaymentInTransaction(payment.getId(), actualAmount, additionalParams);
        } catch (DuplicateOrderIdException e) {
            // DuplicateOrderIdException은 트랜잭션 내부에서 처리
            return self.handleDuplicateOrderIdException(payment.getId(), e, actualAmount);
        } catch (ApiException e) {
            // 즉시 재시도 가능한 오류인지 확인
            if (immediateRetryStrategy.shouldRetry(e, immediateRetryCount)) {
                log.warn("결제 실행 실패, 즉시 재시도 시도: paymentId={}, attempt={}/{}, error={}",
                        payment.getId(), immediateRetryCount + 1, immediateRetryStrategy.getMaxAttempts(), e.getMessage());

                // 재시도 전략에 따른 지연 시간 계산
                long delayMs = immediateRetryStrategy.calculateDelay(immediateRetryCount);
                try {
                    // 트랜잭션 외부에서 지연 처리 (DB 연결 유지 방지)
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, "재시도 중단: " + e.getMessage());
                }

                // 최신 Payment 엔티티를 다시 조회하여 재시도
                Payment freshPayment = self.executePaymentInTransaction(payment.getId(), actualAmount, additionalParams);
                return executePaymentWithImmediateRetry(freshPayment, actualAmount, additionalParams, immediateRetryCount + 1);
            }

            // 즉시 재시도 불가능하거나 최대 횟수 초과 시 예외 전파
            throw e;
        } catch (RuntimeException e) {
            // RuntimeException을 ApiException으로 변환
            // DuplicateOrderIdException은 위에서 이미 처리되므로 여기서는 다른 RuntimeException만 처리
            
            String errorMessage = e.getMessage() != null ? e.getMessage() : "알 수 없는 오류";
            log.error("결제 실행 중 예외 발생: paymentId={}, error={}", payment.getId(), errorMessage, e);
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, "결제 실행 실패: " + errorMessage, e);
        }
    }

    /**
     * 결제 실행 (트랜잭션 내부)
     * 
     * <p>실제 결제 실행 로직을 트랜잭션 내에서 수행합니다.
     * 재시도 로직과 분리하여 DB 연결 유지 문제를 방지합니다.
     *
     * @param paymentId Payment ID
     * @param actualAmount 실제 결제 금액
     * @param additionalParams 추가 파라미터
     * @return 저장된 Payment 엔티티
     */
    @Transactional
    public Payment executePaymentInTransaction(Long paymentId, BigDecimal actualAmount, Map<String, String> additionalParams) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
        
        // 이미 SUCCESS 상태면 외부 API 재호출 금지 (멱등성)
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Payment가 이미 완료 상태: paymentId={}, externalPaymentId={}",
                    payment.getId(), payment.getExternalPaymentId());
            return payment;
        }

        // 멱등성 키 생성 및 저장
        String idempotencyKey = idempotencyService.generateForPayment(payment);
        payment.updateIdempotencyKey(idempotencyKey);

        // Provider 선택 및 결제 승인 호출
        PaymentProvider provider = providerFactory.getProvider(payment.getPaymentMethod());

        // externalPaymentId(paymentKey) 검증
        if (payment.getExternalPaymentId() == null || payment.getExternalPaymentId().isEmpty()) {
            if (payment.getRetryCount() > 0) {
                throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, 
                        "재시도할 수 없습니다. paymentKey가 없습니다. 새로운 결제를 요청해주세요.");
            }
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, 
                    "결제 승인을 위해서는 paymentKey가 필요합니다. /payments/confirm 엔드포인트를 사용해주세요.");
        }

        // Toss Payments의 경우 프론트엔드에서 받은 tossOrderId 사용 (있으면)
        String orderIdForProvider = String.valueOf(payment.getId());
        if (payment.getPaymentMethod() == PaymentMethod.TOSS && additionalParams != null) {
            String tossOrderId = additionalParams.get("tossOrderId");
            if (tossOrderId != null && !tossOrderId.isEmpty()) {
                orderIdForProvider = tossOrderId;
                log.debug("Toss Payments orderId를 프론트엔드에서 받은 값으로 사용: tossOrderId={}, paymentId={}", 
                        tossOrderId, payment.getId());
            }
        }
        
        try {
            PaymentResult result = provider.confirmPayment(
                    payment.getExternalPaymentId(),
                    orderIdForProvider,
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
        } catch (DuplicateOrderIdException e) {
            // S021 오류는 상위로 전파하여 특별 처리
            throw e;
        }
    }

    /**
     * DuplicateOrderIdException 처리 (트랜잭션 내부)
     * 
     * <p>TossPay 중복 주문번호 오류 발생 시 결제 상태를 확인하고 처리합니다.
     */
    @Transactional
    public Payment handleDuplicateOrderIdException(
            Long paymentId,
            DuplicateOrderIdException e,
            BigDecimal actualAmount
    ) {
        // S021 오류: 중복 주문번호 - 결제가 이미 확인되었을 가능성
        log.warn("TossPay 중복 주문번호 오류 발생: paymentId={}, paymentKey={}, orderId={}. " +
                "결제 상태를 확인합니다.", paymentId, e.getPaymentKey(), e.getOrderId());
        
        // 1. 먼저 DB에서 Payment 상태 확인 (webhook으로 이미 처리되었을 수 있음)
        Payment freshPayment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
        
        if (freshPayment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Payment가 이미 SUCCESS 상태입니다 (webhook으로 처리됨): paymentId={}", paymentId);
            return freshPayment;
        }
        
        // 2. TossPayments API에서 실제 결제 상태 조회
        try {
            PaymentProvider statusProvider = providerFactory.getProvider(freshPayment.getPaymentMethod());
            PaymentResult statusResult = statusProvider.getPaymentStatus(freshPayment.getExternalPaymentId());
            
            if (statusResult.isSuccess()) {
                // TossPayments에서 결제가 확인되었음 - DB 업데이트
                log.info("TossPayments에서 결제가 이미 확인되었습니다: paymentId={}, externalPaymentId={}", 
                        paymentId, freshPayment.getExternalPaymentId());
                
                // PaymentResult 검증 및 적용
                paymentValidator.validatePaymentResult(freshPayment, statusResult, actualAmount);
                freshPayment.markSuccess(statusResult.getExternalPaymentId());
                
                return paymentRepository.save(freshPayment);
            } else {
                // TossPayments에서도 실패 상태
                log.warn("TossPayments에서 결제가 실패 상태입니다: paymentId={}, status={}", 
                        paymentId, statusResult.getStatus());
                freshPayment.markFailed("중복 주문번호 오류: " + e.getMessage());
                return paymentRepository.save(freshPayment);
            }
        } catch (Exception statusCheckException) {
            // 상태 조회 실패 - 중복 주문번호 오류로 처리
            log.error("TossPayments 결제 상태 조회 실패: paymentId={}, error={}", 
                    paymentId, statusCheckException.getMessage(), statusCheckException);
            freshPayment.markFailed("중복 주문번호 오류 및 상태 조회 실패: " + e.getMessage());
            return paymentRepository.save(freshPayment);
        }
    }

    /**
     * 결제 취소 실행

     * @return 저장된 Payment 엔티티
     * @throws ApiException 취소 실패 시
     */
    @Transactional
    public Payment executeCancel(Payment payment, String reason) {
        if (payment.getExternalPaymentId() == null) {
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, "외부 결제 ID가 없습니다.");
        }

        String idempotencyKey = idempotencyService.generateForCancel(payment);
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
     * @return 저장된 Payment 엔티티
     * @throws ApiException 환불 실패 시
     */
    @Transactional
    public Payment executeRefund(Payment payment, BigDecimal refundAmount, String reason) {
        if (payment.getExternalPaymentId() == null) {
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, "외부 결제 ID가 없습니다.");
        }

        String idempotencyKey = idempotencyService.generateForRefund(payment);
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
}


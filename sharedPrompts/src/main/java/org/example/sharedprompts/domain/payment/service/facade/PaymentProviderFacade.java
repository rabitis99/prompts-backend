package org.example.sharedprompts.domain.payment.service.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.logging.PaymentLoggingService;
import org.example.sharedprompts.domain.payment.service.payment.provider.PaymentProviderService;
import org.example.sharedprompts.domain.payment.service.payment.provider.PaymentProviderServiceFactory;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 결제사 처리 파사드
 * 결제사별 승인, 취소, 환불, 상태 조회 로직을 담당
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProviderFacade {

    private final PaymentProviderServiceFactory providerServiceFactory;
    private final PaymentLoggingService loggingService;

    /**
     * 결제 승인 처리
     */
    public String approvePayment(Payment payment, BigDecimal actualPaymentAmount) {
        validatePaymentMethod(payment.getPaymentMethod());
        PaymentProviderService providerService = providerServiceFactory.getService(payment.getPaymentMethod());
        
        loggingService.logPaymentApprovalAttempt(payment, payment.getPaymentMethod().name());

        // 실제 결제 금액으로 결제 승인 (포인트 사용 후 금액)
        BigDecimal usedPointAmount = payment.getAmount().subtract(actualPaymentAmount);
        Payment paymentForApproval = Payment.builder()
                .id(payment.getId())
                .user(payment.getUser())
                .amount(actualPaymentAmount) // 포인트 차감 후 실제 결제 금액
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .userType(payment.getUserType())
                .tier(payment.getTier())
                .status(payment.getStatus())
                .metadata(payment.getMetadata())
                .usedPointAmount(usedPointAmount) // 사용된 포인트 금액
                .build();

        return providerService.approvePayment(paymentForApproval);
    }

    /**
     * 결제 취소 처리
     */
    public void cancelPayment(Payment payment, String reason) {
        if (payment.getExternalPaymentId() != null) {
            validatePaymentMethod(payment.getPaymentMethod());
            PaymentProviderService providerService = providerServiceFactory.getService(payment.getPaymentMethod());
            providerService.cancelPayment(payment.getExternalPaymentId(), reason);
        }
    }

    /**
     * 결제 환불 처리
     */
    public void refundPayment(Payment payment, BigDecimal refundAmount, String reason) {
        if (payment.getExternalPaymentId() != null) {
            validatePaymentMethod(payment.getPaymentMethod());
            PaymentProviderService providerService = providerServiceFactory.getService(payment.getPaymentMethod());
            providerService.refundPayment(payment.getExternalPaymentId(), refundAmount, reason);
        }
    }

    /**
     * 결제 상태 조회
     */
    public PaymentStatus checkPaymentStatus(Payment payment) {
        if (payment.getExternalPaymentId() == null) {
            return payment.getStatus();
        }

        try {
            validatePaymentMethod(payment.getPaymentMethod());
            PaymentProviderService providerService = providerServiceFactory.getService(payment.getPaymentMethod());
            return providerService.checkPaymentStatus(payment.getExternalPaymentId());
        } catch (ApiException e) {
            // 비즈니스 예외는 그대로 전파
            throw e;
        } catch (Exception e) {
            // 네트워크 오류 등 예상치 못한 예외만 catch하여 현재 상태 반환
            log.error("결제 상태 조회 실패: paymentId={}, error={}", payment.getId(), e.getMessage(), e);
            return payment.getStatus();
        }
    }

    /**
     * 환불된 금액 조회
     * 
     * <p>외부 결제사 API에서 실제 환불된 금액을 조회합니다.
     * 모든 결제사가 이 기능을 지원하는 것은 아니므로, 지원하지 않는 경우 Optional.empty()를 반환합니다.
     * 
     * @param payment 결제 엔티티
     * @return 환불된 금액 (지원하지 않는 경우 Optional.empty())
     */
    public java.util.Optional<BigDecimal> getRefundedAmount(Payment payment) {
        if (payment.getExternalPaymentId() == null) {
            return java.util.Optional.empty();
        }

        try {
            validatePaymentMethod(payment.getPaymentMethod());
            PaymentProviderService providerService = providerServiceFactory.getService(payment.getPaymentMethod());
            return providerService.getRefundedAmount(payment.getExternalPaymentId());
        } catch (Exception e) {
            log.warn("환불 금액 조회 실패: paymentId={}, error={}", payment.getId(), e.getMessage(), e);
            return java.util.Optional.empty();
        }
    }

    /**
     * PaymentMethod가 null이 아닌지 검증
     */
    private void validatePaymentMethod(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            log.error("PaymentMethod가 null입니다.");
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR,
                    "결제 수단이 지정되지 않았습니다.");
        }
    }
}


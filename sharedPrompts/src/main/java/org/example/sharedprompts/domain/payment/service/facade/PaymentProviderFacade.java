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
        } catch (Exception e) {
            log.error("결제 상태 조회 실패: paymentId={}, error={}", payment.getId(), e.getMessage(), e);
            return payment.getStatus();
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


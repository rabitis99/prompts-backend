package org.example.sharedprompts.domain.payment.application.port.out.paymentgateway;

import org.example.sharedprompts.domain.payment.domain.entity.Payment;

import java.math.BigDecimal;

/**
 * 결제 게이트웨이 아웃포트
 * 외부 결제 제공자와의 통신을 담당하는 인터페이스
 */
public interface PaymentGatewayPort {

    /**
     * 결제를 준비한다 (결제 제공자에서 필요한 경우)
     * 예: 카카오페이 Ready API 호출
     *
     * @param payment 결제 정보
     * @return 결제 준비 결과 (예: TID, orderId 등이 포함)
     */
    PaymentGatewayResult preparePayment(Payment payment);

    /**
     * 결제를 승인한다 (승인 토큰 또는 키를 사용하여)
     *
     * @param payment 결제 정보
     * @param approvalToken 승인 토큰 (제공자별로 다름: paymentKey, tid 등)
     * @return 승인 결과
     */
    PaymentGatewayResult confirmPayment(Payment payment, String approvalToken);

    /**
     * 결제 상태를 조회한다
     *
     * @param externalPaymentId 외부 결제 ID
     * @return 상태 조회 결과
     */
    PaymentGatewayResult queryPaymentStatus(String externalPaymentId);

    /**
     * 결제를 취소한다
     *
     * @param payment 결제 정보
     * @return 취소 결과
     */
    PaymentGatewayResult cancelPayment(Payment payment);

    /**
     * 결제를 환불한다
     *
     * @param payment 결제 정보
     * @param refundAmount 환불 금액 (null이면 전체 환불)
     * @return 환불 결과
     */
    PaymentGatewayResult refundPayment(Payment payment, BigDecimal refundAmount);

    /**
     * 거래 수수료 또는 추가 정보 조회
     */
    PaymentGatewayResult queryTransactionDetails(String externalPaymentId);

    /**
     * 결제 게이트웨이 결과 DTO
     */
    class PaymentGatewayResult {
        public final boolean success;
        public final String externalPaymentId;
        public final String approvalCode;
        public final String message;
        public final Exception exception;

        public boolean isSuccess() {
            return success;
        }

        public String getExternalPaymentId() {
            return externalPaymentId;
        }

        public String getApprovalCode() {
            return approvalCode;
        }

        public String getMessage() {
            return message;
        }

        public String getErrorMessage() {
            return success ? null : message;
        }

        public Exception getException() {
            return exception;
        }

        public PaymentGatewayResult(boolean success, String externalPaymentId, String approvalCode, String message) {
            this.success = success;
            this.externalPaymentId = externalPaymentId;
            this.approvalCode = approvalCode;
            this.message = message;
            this.exception = null;
        }

        public PaymentGatewayResult(boolean success, String message, Exception exception) {
            this.success = success;
            this.externalPaymentId = null;
            this.approvalCode = null;
            this.message = message;
            this.exception = exception;
        }

        public static PaymentGatewayResult success(String externalPaymentId, String approvalCode) {
            return new PaymentGatewayResult(true, externalPaymentId, approvalCode, "Success");
        }

        public static PaymentGatewayResult failure(String message, Exception exception) {
            return new PaymentGatewayResult(false, message, exception);
        }
    }
}

package org.example.sharedprompts.domain.payment.service.core;

import org.example.sharedprompts.dto.payment.request.PaymentCancelRequestDto;
import org.example.sharedprompts.dto.payment.request.PaymentRefundRequestDto;
import org.example.sharedprompts.dto.payment.request.PaymentRequestDto;
import org.example.sharedprompts.dto.payment.response.PaymentResponseDto;
import org.example.sharedprompts.dto.payment.response.PaymentStatusResponseDto;

/**
 * 결제 서비스 인터페이스
 */
public interface PaymentService {

    /**
 * Initiates a payment for the specified user and enforces tier-based daily limits.
 *
 * @param userId the identifier of the user initiating the payment
 * @param request details of the payment to be processed (amount, method, metadata)
 * @return a PaymentResponseDto containing the payment identifier, status, processed amount, and any failure reason
 */
    PaymentResponseDto requestPayment(Long userId, PaymentRequestDto request);

    /**
 * Retrieve the current status of a payment by its identifier.
 *
 * @param paymentId the unique identifier of the payment to query
 * @return the payment's current status and associated details as a PaymentStatusResponseDto
 */
    PaymentStatusResponseDto checkPaymentStatus(String paymentId);

    /**
 * Cancel a previously submitted payment for the specified user.
 *
 * @param userId the identifier of the user who owns the payment
 * @param request cancellation details required to identify and process the payment cancellation
 * @return a PaymentResponseDto containing the updated payment status and related metadata
 */
    PaymentResponseDto cancelPayment(Long userId, PaymentCancelRequestDto request);

    /**
 * Processes a partial or full refund for a user's payment.
 *
 * @param userId the ID of the user who owns the payment to be refunded
 * @param request details of the refund operation (including payment identifier and refund amount)
 * @return a PaymentResponseDto containing the updated payment status and refund details
 */
    PaymentResponseDto refundPayment(Long userId, PaymentRefundRequestDto request);

    /**
 * Retrieves the payment history for the specified user.
 *
 * @param userId the identifier of the user whose payment history is requested
 * @return a list of PaymentResponseDto objects representing the user's past payments
 */
    java.util.List<PaymentResponseDto> getPaymentHistory(Long userId);
}

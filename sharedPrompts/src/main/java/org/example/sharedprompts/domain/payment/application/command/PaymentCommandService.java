package org.example.sharedprompts.domain.payment.application.command;

import org.example.sharedprompts.dto.payment.request.PaymentCancelRequestDto;
import org.example.sharedprompts.dto.payment.request.PaymentConfirmRequest;
import org.example.sharedprompts.dto.payment.request.PaymentRefundRequestDto;
import org.example.sharedprompts.dto.payment.request.PaymentRequestDto;
import org.example.sharedprompts.dto.payment.response.PaymentConfirmResponse;
import org.example.sharedprompts.dto.payment.response.PaymentResponseDto;
import org.example.sharedprompts.dto.payment.response.PaymentStatusResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentCommandService {

    PaymentResponseDto requestPayment(Long userId, PaymentRequestDto request);

    PaymentStatusResponseDto checkPaymentStatus(Long paymentId, Long userId);

    PaymentResponseDto cancelPayment(Long userId, PaymentCancelRequestDto request);

    PaymentResponseDto refundPayment(Long userId, PaymentRefundRequestDto request);

    Page<PaymentResponseDto> getPaymentHistory(Long userId, Pageable pageable);

    PaymentConfirmResponse confirmPayment(Long userId, PaymentConfirmRequest request);
}


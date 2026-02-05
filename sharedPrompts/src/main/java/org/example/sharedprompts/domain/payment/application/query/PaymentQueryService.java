package org.example.sharedprompts.domain.payment.application.query;

import org.example.sharedprompts.dto.payment.response.PaymentResponseDto;
import org.example.sharedprompts.dto.payment.response.PaymentStatusResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentQueryService {

    PaymentStatusResponseDto checkPaymentStatus(Long paymentId, Long userId);

    Page<PaymentResponseDto> getPaymentHistory(Long userId, Pageable pageable);
}


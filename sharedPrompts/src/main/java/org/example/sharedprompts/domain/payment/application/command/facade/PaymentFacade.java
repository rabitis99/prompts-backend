package org.example.sharedprompts.domain.payment.application.command.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.application.command.PaymentCommandService;
import org.example.sharedprompts.domain.payment.application.command.service.PaymentConfirmService;
import org.example.sharedprompts.domain.payment.application.query.PaymentStatusSyncService;
import org.example.sharedprompts.domain.payment.application.command.service.*;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.infrastructure.monitoring.PaymentLoggingService;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.PaymentJpaAdapter;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.dto.payment.request.PaymentCancelRequestDto;
import org.example.sharedprompts.dto.payment.request.PaymentConfirmRequest;
import org.example.sharedprompts.dto.payment.request.PaymentRefundRequestDto;
import org.example.sharedprompts.dto.payment.request.PaymentRequestDto;
import org.example.sharedprompts.dto.payment.response.PaymentConfirmResponse;
import org.example.sharedprompts.dto.payment.response.PaymentResponseDto;
import org.example.sharedprompts.dto.payment.response.PaymentStatusResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentFacade implements PaymentCommandService {

    private final UserRepository userRepository;
    private final PaymentRequestService requestService;
    private final PaymentConfirmService confirmService;
    private final PaymentCancelService cancelService;
    private final PaymentRefundService refundService;
    private final PaymentStatusSyncService statusSyncService;
    private final PaymentJpaAdapter paymentJpaAdapter;
    private final PaymentLoggingService loggingService;

    @Override
    @Transactional
    public PaymentResponseDto requestPayment(Long userId, PaymentRequestDto request) {
        String traceId = null;

        try {
            traceId = loggingService.startTrace(null, userId);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

            Payment payment = requestService.processRequest(userId, request, user);

            if (traceId != null && payment.getId() != null) {
                loggingService.startTrace(payment.getId(), userId);
            }

            return PaymentResponseDto.from(payment);
        } finally {
            loggingService.endTrace();
        }
    }

    @Override
    @Transactional
    public PaymentStatusResponseDto checkPaymentStatus(Long paymentId, Long userId) {
        return statusSyncService.syncPaymentStatus(paymentId, userId);
    }

    @Override
    @Transactional
    public PaymentResponseDto cancelPayment(Long userId, PaymentCancelRequestDto request) {
        Payment canceledPayment = cancelService.cancel(userId, request);
        return PaymentResponseDto.from(canceledPayment);
    }

    @Override
    @Transactional
    public PaymentResponseDto refundPayment(Long userId, PaymentRefundRequestDto request) {
        Payment refundedPayment = refundService.refund(userId, request);
        return PaymentResponseDto.from(refundedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponseDto> getPaymentHistory(Long userId, Pageable pageable) {
        return paymentJpaAdapter.findByUserIdWithFetchJoin(userId, pageable)
                .map(PaymentResponseDto::from);
    }

    @Override
    @Transactional
    public PaymentConfirmResponse confirmPayment(Long userId, PaymentConfirmRequest request) {
        return confirmService.confirm(userId, request);
    }
}


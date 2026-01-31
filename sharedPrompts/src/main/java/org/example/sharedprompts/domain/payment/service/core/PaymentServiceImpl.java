package org.example.sharedprompts.domain.payment.service.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.logging.PaymentLoggingService;
import org.example.sharedprompts.domain.payment.repository.payment.PaymentRepository;
import org.example.sharedprompts.domain.payment.service.facade.AmountProcessingResult;
import org.example.sharedprompts.domain.payment.service.facade.PaymentAmountFacade;
import org.example.sharedprompts.domain.payment.service.execution.PaymentExecutionService;
import org.example.sharedprompts.domain.payment.service.postprocess.PaymentPostProcessService;
import org.example.sharedprompts.domain.payment.service.sync.PaymentStatusSyncService;
import org.example.sharedprompts.domain.payment.service.validation.PaymentValidationService;
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

import java.math.BigDecimal;

/**
 * 결제 서비스 구현체
 * 파사드 패턴을 사용하여 복잡한 로직을 분리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final PaymentValidationService validationService;
    private final PaymentAmountFacade amountFacade;
    private final PaymentExecutionService executionService;
    private final PaymentPostProcessService postProcessService;
    private final PaymentStatusSyncService statusSyncService;
    private final PaymentLoggingService loggingService;

    @Override
    @Transactional
    public PaymentResponseDto requestPayment(Long userId, PaymentRequestDto request) {
        String traceId = null;

        try {
            traceId = loggingService.startTrace(null, userId);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

            validationService.validateDailyLimit(userId, user.getTier());

            AmountProcessingResult amountResult =
                    amountFacade.processPaymentAmount(userId, request);

            Payment payment = request.toPaymentBuilder(
                    user,
                    user.getTier(),
                    amountResult.getConvertedAmount(),
                    amountResult.getUsedPointAmount()
            ).build();

            payment = paymentRepository.save(payment);
            loggingService.logPaymentRequest(payment);

            // 결제 요청은 Payment 엔티티만 생성하고 PENDING 상태로 유지
            // 실제 결제 승인은 /payments/confirm 엔드포인트를 통해 클라이언트에서 받은 paymentKey로 진행
            // 이렇게 하면 토스페이먼츠, 카카오페이 등 결제사별 올바른 플로우를 따를 수 있음

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
        Payment payment = paymentRepository.findById(request.getPaymentIdAsLong())
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

        validationService.validatePaymentOwnership(payment, userId);
        validationService.validateCancelableStatus(payment);

        try {
            PaymentStatus oldStatus = payment.getStatus();

            // PaymentExecutionService를 통한 취소 실행
            payment = executionService.executeCancel(payment, request.getReasonOrDefault());

            postProcessService.processPaymentCancel(payment, userId, request.getReasonOrDefault(), oldStatus);

        } catch (Exception e) {
            log.error("결제 취소 실패: paymentId={}, error={}", request.getPaymentId(), e.getMessage(), e);
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR);
        }

        return PaymentResponseDto.from(payment);
    }

    @Override
    @Transactional
    public PaymentResponseDto refundPayment(Long userId, PaymentRefundRequestDto request) {
        Payment payment = paymentRepository.findById(request.getPaymentIdAsLong())
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

        validationService.validatePaymentOwnership(payment, userId);
        validationService.validateRefundableStatus(payment);
        BigDecimal refundAmount = validationService.validateRefundAmount(request.getAmount(), payment);

        try {
            PaymentStatus oldStatus = payment.getStatus();

            // PaymentExecutionService를 통한 환불 실행
            payment = executionService.executeRefund(payment, refundAmount, request.getReasonOrDefault());

            BigDecimal refundPointAmount = amountFacade.calculateRefundPointAmount(
                    payment.getUsedPointAmount(),
                    payment.getAmount(),
                    refundAmount
            );

            postProcessService.processPaymentRefund(
                    payment,
                    userId,
                    refundAmount,
                    refundPointAmount,
                    request.getReasonOrDefault(),
                    oldStatus
            );

        } catch (Exception e) {
            log.error("결제 환불 실패: paymentId={}, error={}", request.getPaymentId(), e.getMessage(), e);
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR);
        }

        return PaymentResponseDto.from(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponseDto> getPaymentHistory(Long userId, Pageable pageable) {
        return paymentRepository.findByUserIdWithFetchJoin(userId, pageable)
                .map(PaymentResponseDto::from);
    }

    @Override
    @Transactional
    public PaymentConfirmResponse confirmPayment(Long userId, PaymentConfirmRequest request) {
        long startTime = System.currentTimeMillis();

        Payment payment = paymentRepository.findById(request.getOrderIdAsLong())
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

        validationService.validatePaymentOwnership(payment, userId);

        // 클라이언트에서 받은 paymentKey를 externalPaymentId로 설정
        if (request.getPaymentKey() != null && !request.getPaymentKey().isEmpty()) {
            payment.updateExternalPaymentId(request.getPaymentKey());
        }

        // 실제 결제 금액 계산 (포인트 사용 후 금액)
        BigDecimal actualAmount = payment.getAmount().subtract(
                payment.getUsedPointAmount() != null ? payment.getUsedPointAmount() : BigDecimal.ZERO
        );

        try {
            // PaymentExecutionService를 통한 결제 실행
            payment = executionService.executePayment(payment, actualAmount);

            long processingTime = System.currentTimeMillis() - startTime;

            try {
                postProcessService.processPaymentSuccess(
                        payment,
                        userId,
                        actualAmount,
                        payment.getAmount(),
                        processingTime
                );
            } catch (Exception postProcessException) {
                log.error("결제 승인 성공 후 후처리 실패: paymentId={}, userId={}, error={}",
                        payment.getId(), userId, postProcessException.getMessage(), postProcessException);
            }

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            payment.fail("결제 승인 실패: " + e.getMessage());
            payment = paymentRepository.save(payment);

            try {
                postProcessService.processPaymentFailure(
                        payment,
                        userId,
                        e.getMessage(),
                        e,
                        processingTime
                );
            } catch (Exception postProcessException) {
                log.error("결제 실패 후처리 중 오류 발생: paymentId={}, userId={}, error={}",
                        payment.getId(), userId, postProcessException.getMessage(), postProcessException);
            }
            throw e;
        }

        PaymentConfirmResponse response = new PaymentConfirmResponse();
        response.setPaymentKey(payment.getExternalPaymentId());
        response.setOrderId(request.getOrderId());
        response.setStatus(payment.getStatus().name());
        response.setTotalAmount(payment.getAmount().intValue());
        if (payment.getApprovedAt() != null) {
            response.setApprovedAt(payment.getApprovedAt().atZone(java.time.ZoneId.systemDefault()).toOffsetDateTime());
        }
        response.setMethod(payment.getPaymentMethod().name());

        return response;
    }


    @Override
    @Transactional
    public PaymentStatusResponseDto checkPaymentStatusForAdmin(Long paymentId) {
        return statusSyncService.syncPaymentStatusForAdmin(paymentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponseDto> getAllPaymentHistory(Pageable pageable) {
        return paymentRepository.findAllWithFetchJoin(pageable)
                .map(PaymentResponseDto::from);
    }

    @Override
    @Transactional
    public PaymentResponseDto cancelPaymentForAdmin(Long paymentId, PaymentCancelRequestDto request, Long adminId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

        // 관리자는 소유권 검증 없이 취소 가능
        validationService.validateCancelableStatus(payment);

        try {
            PaymentStatus oldStatus = payment.getStatus();

            // PaymentExecutionService를 통한 취소 실행
            payment = executionService.executeCancel(payment, request.getReasonOrDefault());

            postProcessService.processPaymentCancel(payment, payment.getUser().getId(), request.getReasonOrDefault(), oldStatus);

        } catch (Exception e) {
            log.error("관리자 결제 취소 실패: paymentId={}, adminId={}, error={}", paymentId, adminId, e.getMessage(), e);
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR);
        }

        return PaymentResponseDto.from(payment);
    }

    @Override
    @Transactional
    public PaymentResponseDto refundPaymentForAdmin(Long paymentId, PaymentRefundRequestDto request, Long adminId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

        // 관리자는 소유권 검증 없이 환불 가능
        validationService.validateRefundableStatus(payment);
        BigDecimal refundAmount = validationService.validateRefundAmount(request.getAmount(), payment);

        try {
            PaymentStatus oldStatus = payment.getStatus();

            // PaymentExecutionService를 통한 환불 실행
            payment = executionService.executeRefund(payment, refundAmount, request.getReasonOrDefault());

            BigDecimal refundPointAmount = amountFacade.calculateRefundPointAmount(
                    payment.getUsedPointAmount(),
                    payment.getAmount(),
                    refundAmount
            );

            postProcessService.processPaymentRefund(
                    payment,
                    payment.getUser().getId(),
                    refundAmount,
                    refundPointAmount,
                    request.getReasonOrDefault(),
                    oldStatus
            );

        } catch (Exception e) {
            log.error("관리자 결제 환불 실패: paymentId={}, adminId={}, error={}", paymentId, adminId, e.getMessage(), e);
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR);
        }

        return PaymentResponseDto.from(payment);
    }

}

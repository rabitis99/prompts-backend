package org.example.sharedprompts.domain.payment.service.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.logging.PaymentLoggingService;
import org.example.sharedprompts.domain.payment.repository.PaymentRepository;
import org.example.sharedprompts.domain.payment.service.facade.PaymentAmountFacade;
import org.example.sharedprompts.domain.payment.service.facade.PaymentPostProcessFacade;
import org.example.sharedprompts.domain.payment.service.facade.PaymentProviderFacade;
import org.example.sharedprompts.domain.payment.service.facade.PaymentValidationFacade;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.dto.payment.request.PaymentCancelRequestDto;
import org.example.sharedprompts.dto.payment.request.PaymentRefundRequestDto;
import org.example.sharedprompts.dto.payment.request.PaymentRequestDto;
import org.example.sharedprompts.dto.payment.response.PaymentResponseDto;
import org.example.sharedprompts.dto.payment.response.PaymentStatusResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

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
    private final PaymentValidationFacade validationFacade;
    private final PaymentAmountFacade amountFacade;
    private final PaymentProviderFacade providerFacade;
    private final PaymentPostProcessFacade postProcessFacade;
    private final PaymentLoggingService loggingService;

    /**
     * Initiates and processes a payment request for a user.
     *
     * Performs validation, computes and applies payment amounts, persists a Payment entity,
     * attempts external approval, executes success or failure post-processing, updates tracing,
     * and returns a DTO representing the final payment state.
     *
     * @param userId the identifier of the user initiating the payment
     * @param request the payment request data
     * @return a PaymentResponseDto representing the persisted payment and its final status
     * @throws ApiException if the user with the given id is not found
     */
    @Override
    @Transactional
    public PaymentResponseDto requestPayment(Long userId, PaymentRequestDto request) {
        long startTime = System.currentTimeMillis();
        String traceId = null;
        
        try {
            // 트레이싱 시작
            traceId = loggingService.startTrace(null, userId);
            
            // 사용자 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

            // 티어 기반 일일 결제 제한 체크
            validationFacade.validateDailyLimit(userId, user.getTier());

            // 금액 처리 (환율 변환 + 포인트 사용)
            PaymentAmountFacade.AmountProcessingResult amountResult = 
                    amountFacade.processPaymentAmount(userId, request);

            // 결제 엔티티 생성 (RequestDto의 mapper 사용)
            Payment payment = request.toPaymentBuilder(
                    user,
                    user.getTier(),
                    amountResult.getConvertedAmount(),
                    amountResult.getUsedPointAmount()
            ).build();

            payment = paymentRepository.save(payment);
            loggingService.logPaymentRequest(payment);

            try {
                // 결제사별 승인 처리
                String externalPaymentId = providerFacade.approvePayment(
                        payment, 
                        amountResult.getActualPaymentAmount()
                );
                payment.approve(externalPaymentId);

                long processingTime = System.currentTimeMillis() - startTime;
                
                // 결제 성공 후처리
                postProcessFacade.processPaymentSuccess(
                        payment,
                        userId,
                        amountResult.getActualPaymentAmount(),
                        amountResult.getConvertedAmount(),
                        processingTime
                );

            } catch (Exception e) {
                long processingTime = System.currentTimeMillis() - startTime;
                payment.fail("결제 승인 실패: " + e.getMessage());
                
                // 결제 실패 후처리
                postProcessFacade.processPaymentFailure(
                        payment,
                        userId,
                        e.getMessage(),
                        e,
                        processingTime
                );
            }

            payment = paymentRepository.save(payment);
            
            // 트레이싱 ID 업데이트
            if (traceId != null && payment.getId() != null) {
                loggingService.startTrace(payment.getId(), userId);
            }
            
            return PaymentResponseDto.from(payment);
        } finally {
            loggingService.endTrace();
        }
    }

    /**
     * Checks and synchronizes a payment's status with the external provider.
     *
     * Queries the external payment provider for the latest status of the payment identified by the given ID,
     * updates the local payment record when the external status differs (approving locally if the provider
     * reports success for a pending payment), and returns the current payment status representation.
     *
     * @param paymentId the payment identifier as a numeric string
     * @return the current PaymentStatusResponseDto representing the stored payment state
     * @throws ApiException with ErrorCode.PAYMENT_NOT_FOUND if no payment exists for the given ID
     */
    @Override
    @Transactional
    public PaymentStatusResponseDto checkPaymentStatus(String paymentId) {
        Payment payment = paymentRepository.findById(Long.parseLong(paymentId))
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

        // 외부 결제사에서 최신 상태 조회
        PaymentStatus latestStatus = providerFacade.checkPaymentStatus(payment);
        
        // 상태가 변경된 경우 업데이트
        if (payment.getStatus() != latestStatus) {
            payment = paymentRepository.findById(payment.getId()).orElse(payment);
            if (latestStatus == PaymentStatus.SUCCESS && payment.getStatus() == PaymentStatus.PENDING) {
                payment.approve(payment.getExternalPaymentId());
                paymentRepository.save(payment);
            }
        }

        return PaymentStatusResponseDto.from(payment);
    }

    /**
     * Cancels a payment owned by the specified user, performs provider-side cancellation, persists the change,
     * and runs post-cancellation processing.
     *
     * @param userId  the id of the user requesting the cancellation
     * @param request the cancellation request containing the payment id and optional reason (a default reason is used when absent)
     * @return a DTO representing the updated payment
     * @throws ApiException when the payment is not found, the user is not the owner, the payment is not cancelable,
     *                      or the external payment provider fails (error codes: PAYMENT_NOT_FOUND, PAYMENT_PROVIDER_ERROR)
     */
    @Override
    @Transactional
    public PaymentResponseDto cancelPayment(Long userId, PaymentCancelRequestDto request) {
        Payment payment = paymentRepository.findById(request.getPaymentIdAsLong())
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

        // 검증
        validationFacade.validatePaymentOwnership(payment, userId);
        validationFacade.validateCancelableStatus(payment);

        try {
            PaymentStatus oldStatus = payment.getStatus();
            
            // 결제사별 취소 처리
            providerFacade.cancelPayment(payment, request.getReasonOrDefault());

            payment.cancel();
            payment = paymentRepository.save(payment);

            // 결제 취소 후처리
            postProcessFacade.processPaymentCancel(payment, userId, request.getReasonOrDefault(), oldStatus);

        } catch (Exception e) {
            log.error("결제 취소 실패: paymentId={}, error={}", request.getPaymentId(), e.getMessage(), e);
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR);
        }

        return PaymentResponseDto.from(payment);
    }

    /**
     * Processes a refund for an existing payment and returns the updated payment details.
     *
     * @param userId the ID of the user requesting the refund
     * @param request details of the refund request (payment id, amount, reason)
     * @return a PaymentResponseDto representing the payment after the refund
     * @throws ApiException if the payment is not found or if the external payment provider fails
     */
    @Override
    @Transactional
    public PaymentResponseDto refundPayment(Long userId, PaymentRefundRequestDto request) {
        Payment payment = paymentRepository.findById(request.getPaymentIdAsLong())
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

        // 검증
        validationFacade.validatePaymentOwnership(payment, userId);
        validationFacade.validateRefundableStatus(payment);
        BigDecimal refundAmount = validationFacade.validateRefundAmount(request.getAmountOrNull(), payment);

        try {
            // 결제사별 환불 처리
            providerFacade.refundPayment(payment, refundAmount, request.getReasonOrDefault());

            PaymentStatus oldStatus = payment.getStatus();
            payment.refund(refundAmount);
            payment = paymentRepository.save(payment);

            // 환불 시 포인트 환불 금액 계산
            BigDecimal refundPointAmount = amountFacade.calculateRefundPointAmount(
                    payment.getUsedPointAmount(),
                    payment.getAmount(),
                    refundAmount
            );

            // 결제 환불 후처리
            postProcessFacade.processPaymentRefund(
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

    /**
     * Retrieve a user's payment history with the most recent payments first.
     *
     * Each payment is converted to a PaymentResponseDto.
     *
     * @param userId the identifier of the user whose payment history to fetch
     * @return a list of PaymentResponseDto ordered by creation time descending (newest first)
     */
    @Override
    public List<PaymentResponseDto> getPaymentHistory(Long userId) {
        List<Payment> payments = paymentRepository.findByUser_IdOrderByCreatedAtDesc(userId);
        return payments.stream()
                .map(PaymentResponseDto::from)
                .collect(Collectors.toList());
    }
}
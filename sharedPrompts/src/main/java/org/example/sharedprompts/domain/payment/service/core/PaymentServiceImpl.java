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

    @Override
    @Transactional
    public PaymentStatusResponseDto checkPaymentStatus(Long paymentId, Long userId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

        validationFacade.validatePaymentOwnership(payment, userId);
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

    @Override
    public List<PaymentResponseDto> getPaymentHistory(Long userId) {
        List<Payment> payments = paymentRepository.findByUser_IdOrderByCreatedAtDesc(userId);
        return payments.stream()
                .map(PaymentResponseDto::from)
                .collect(Collectors.toList());
    }
}

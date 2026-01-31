package org.example.sharedprompts.domain.payment.service.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.logging.PaymentLoggingService;
import org.example.sharedprompts.domain.payment.repository.payment.PaymentRepository;
import org.example.sharedprompts.domain.payment.service.facade.PaymentAmountFacade;
import org.example.sharedprompts.domain.payment.service.facade.PaymentPostProcessFacade;
import org.example.sharedprompts.domain.payment.service.facade.PaymentProviderFacade;
import org.example.sharedprompts.domain.payment.service.facade.PaymentValidationFacade;
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
                payment = paymentRepository.save(payment); // 승인 성공 상태 저장

                long processingTime = System.currentTimeMillis() - startTime;
                
                // 결제 성공 후처리 (포인트 적립, 캐시백 등)
                // 후처리 실패는 결제 상태에 영향을 주지 않도록 별도 처리
                try {
                    postProcessFacade.processPaymentSuccess(
                            payment,
                            userId,
                            amountResult.getActualPaymentAmount(),
                            amountResult.getConvertedAmount(),
                            processingTime
                    );
                } catch (Exception postProcessException) {
                    // 후처리 실패는 로깅만 하고 결제 상태는 성공으로 유지
                    // 필요시 재시도 가능하도록 별도 처리
                    log.error("결제 승인 성공 후 후처리 실패: paymentId={}, userId={}, error={}", 
                            payment.getId(), userId, postProcessException.getMessage(), postProcessException);
                    // TODO: 후처리 실패에 대한 재시도/보상 트랜잭션 처리 고려
                }

            } catch (Exception e) {
                long processingTime = System.currentTimeMillis() - startTime;
                payment.fail("결제 승인 실패: " + e.getMessage());
                payment = paymentRepository.save(payment); // 실패 상태 저장
                
                // 결제 실패 후처리
                try {
                    postProcessFacade.processPaymentFailure(
                            payment,
                            userId,
                            e.getMessage(),
                            e,
                            processingTime
                    );
                } catch (Exception postProcessException) {
                    // 실패 후처리도 실패한 경우 로깅만 수행
                    log.error("결제 실패 후처리 중 오류 발생: paymentId={}, userId={}, error={}", 
                            payment.getId(), userId, postProcessException.getMessage(), postProcessException);
                }
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
            syncPaymentStatus(payment, latestStatus);
            paymentRepository.save(payment);
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
    @Transactional(readOnly = true)
    public Page<PaymentResponseDto> getPaymentHistory(Long userId, Pageable pageable) {
        return paymentRepository.findByUserIdWithFetchJoin(userId, pageable)
                .map(PaymentResponseDto::from);
    }

    @Override
    @Transactional
    public PaymentConfirmResponse confirmPayment(Long userId, PaymentConfirmRequest request) {
        // 결제 조회 및 소유권 검증
        Payment payment = paymentRepository.findById(Long.parseLong(request.getOrderId()))
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
        
        validationFacade.validatePaymentOwnership(payment, userId);
        
        // 결제사별 승인 처리
        String externalPaymentId = providerFacade.approvePayment(
                payment,
                BigDecimal.valueOf(request.getAmount())
        );
        
        payment.approve(externalPaymentId);
        payment = paymentRepository.save(payment);
        
        // PaymentConfirmResponse 생성
        PaymentConfirmResponse response = new PaymentConfirmResponse();
        response.setPaymentKey(externalPaymentId);
        response.setOrderId(request.getOrderId());
        response.setStatus(payment.getStatus().name());
        response.setTotalAmount(payment.getAmount().intValue());
        response.setApprovedAt(payment.getApprovedAt());
        response.setMethod(payment.getPaymentMethod().name());
        
        return response;
    }

    // ============ 관리자용 메서드 ============

    @Override
    @Transactional
    public PaymentStatusResponseDto checkPaymentStatusForAdmin(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

        // 관리자는 소유권 검증 없이 조회 가능
        // 외부 결제사에서 최신 상태 조회
        PaymentStatus latestStatus = providerFacade.checkPaymentStatus(payment);
        
        // 상태가 변경된 경우 업데이트
        if (payment.getStatus() != latestStatus) {
            payment = paymentRepository.findById(payment.getId()).orElse(payment);
            syncPaymentStatus(payment, latestStatus);
            paymentRepository.save(payment);
        }

        return PaymentStatusResponseDto.from(payment);
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
        validationFacade.validateCancelableStatus(payment);

        try {
            PaymentStatus oldStatus = payment.getStatus();
            
            // 결제사별 취소 처리
            providerFacade.cancelPayment(payment, request.getReasonOrDefault());

            payment.cancel();
            payment = paymentRepository.save(payment);

            // 결제 취소 후처리 (관리자 ID 전달)
            postProcessFacade.processPaymentCancel(payment, payment.getUser().getId(), request.getReasonOrDefault(), oldStatus);

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

            // 결제 환불 후처리 (관리자 ID 전달)
            postProcessFacade.processPaymentRefund(
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

    // ============ Helper Methods ============

    /**
     * 외부 결제사 상태와 DB 상태를 동기화합니다.
     * 모든 상태 전이를 처리하여 외부 상태 변경이 DB에 완전히 반영되도록 합니다.
     */
    private void syncPaymentStatus(Payment payment, PaymentStatus latestStatus) {
        PaymentStatus currentStatus = payment.getStatus();
        
        // 이미 동일한 상태면 처리하지 않음
        if (currentStatus == latestStatus) {
            return;
        }

        switch (latestStatus) {
            case SUCCESS:
                // PENDING -> SUCCESS 전이만 처리
                if (currentStatus == PaymentStatus.PENDING) {
                    payment.approve(payment.getExternalPaymentId());
                    log.info("외부 결제사 상태 동기화: paymentId={}, {} -> {}", 
                            payment.getId(), currentStatus, latestStatus);
                }
                break;
                
            case FAILED:
                // PENDING -> FAILED 전이만 처리
                if (currentStatus == PaymentStatus.PENDING) {
                    payment.fail("외부 결제사에서 결제 실패로 확인됨");
                    log.info("외부 결제사 상태 동기화: paymentId={}, {} -> {}", 
                            payment.getId(), currentStatus, latestStatus);
                }
                break;
                
            case CANCELED:
                // SUCCESS, PENDING -> CANCELED 전이 처리
                if (currentStatus == PaymentStatus.SUCCESS || currentStatus == PaymentStatus.PENDING) {
                    payment.cancel();
                    log.info("외부 결제사 상태 동기화: paymentId={}, {} -> {}", 
                            payment.getId(), currentStatus, latestStatus);
                }
                break;
                
            case REFUNDED:
                // SUCCESS, PARTIALLY_REFUNDED -> REFUNDED 전이 처리
                if (currentStatus == PaymentStatus.SUCCESS || currentStatus == PaymentStatus.PARTIALLY_REFUNDED) {
                    // 전체 환불 처리 (남은 금액 전부 환불)
                    BigDecimal remainingAmount = payment.getRefundableAmount();
                    if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
                        payment.refund(remainingAmount);
                        log.info("외부 결제사 상태 동기화: paymentId={}, {} -> {}, 환불금액={}", 
                                payment.getId(), currentStatus, latestStatus, remainingAmount);
                    }
                }
                break;
                
            case PARTIALLY_REFUNDED:
                // SUCCESS -> PARTIALLY_REFUNDED 전이 처리
                if (currentStatus == PaymentStatus.SUCCESS) {
                    // 부분 환불 처리
                    // 외부 결제사에서 부분 환불 금액을 직접 조회할 수 없는 경우,
                    // 현재 DB의 refundedAmount가 0이면 최소 환불 금액(1원)을 환불 처리하여 상태만 동기화
                    // 실제 환불 금액은 외부 결제사 API에서 조회하거나, 별도 환불 처리 API를 통해 정확히 반영해야 함
                    BigDecimal currentRefundedAmount = payment.getRefundedAmount();
                    if (currentRefundedAmount.compareTo(BigDecimal.ZERO) == 0) {
                        // 아직 환불이 없었다면 최소 금액(1원)을 환불하여 상태만 동기화
                        // 실제 환불 금액은 외부 API에서 조회하여 별도로 업데이트 필요
                        BigDecimal minRefundAmount = BigDecimal.ONE;
                        payment.refund(minRefundAmount);
                        log.warn("외부 결제사 상태 동기화: paymentId={}, {} -> {}, 부분 환불 상태만 동기화 (실제 환불 금액은 외부 API에서 조회 필요)", 
                                payment.getId(), currentStatus, latestStatus);
                    } else {
                        // 이미 환불이 있었다면 상태만 업데이트
                        // refund 메서드는 금액을 더하므로, 현재 환불 금액이 전체 금액보다 작은 경우에만 호출
                        BigDecimal refundableAmount = payment.getRefundableAmount();
                        if (refundableAmount.compareTo(BigDecimal.ZERO) > 0) {
                            // 최소 금액을 환불하여 상태만 동기화 (실제 환불 금액은 외부 API에서 조회 필요)
                            payment.refund(BigDecimal.ONE);
                            log.warn("외부 결제사 상태 동기화: paymentId={}, {} -> {}, 부분 환불 상태만 동기화 (기존 환불 금액={}, 실제 환불 금액은 외부 API에서 조회 필요)", 
                                    payment.getId(), currentStatus, latestStatus, currentRefundedAmount);
                        }
                    }
                }
                break;
                
            case PENDING:
                // 일반적으로 외부에서 PENDING으로 되돌아가는 경우는 없지만, 로깅만 수행
                log.warn("외부 결제사 상태가 PENDING으로 변경됨: paymentId={}, 현재 상태={}", 
                        payment.getId(), currentStatus);
                break;
                
            default:
                log.warn("알 수 없는 결제 상태: paymentId={}, 상태={}", payment.getId(), latestStatus);
                break;
        }
    }
}

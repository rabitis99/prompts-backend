package org.example.sharedprompts.domain.payment.service.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.logging.PaymentLoggingService;
import org.example.sharedprompts.domain.payment.provider.PaymentProvider;
import org.example.sharedprompts.domain.payment.provider.PaymentProviderFactory;
import org.example.sharedprompts.domain.payment.repository.payment.PaymentRepository;
import org.example.sharedprompts.domain.payment.service.facade.AmountProcessingResult;
import org.example.sharedprompts.domain.payment.service.facade.PaymentAmountFacade;
import org.example.sharedprompts.domain.payment.service.execution.PaymentExecutionService;
import org.example.sharedprompts.domain.payment.service.postprocess.PaymentPostProcessService;
import org.example.sharedprompts.domain.payment.service.sync.PaymentStatusSyncService;
import org.example.sharedprompts.domain.payment.service.validation.PaymentValidationService;
import org.example.sharedprompts.domain.payment.validator.PaymentValidator;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.Collections;
import java.util.Map;

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
    private final PaymentProviderFactory providerFactory;
    private final ObjectMapper objectMapper;
    private final PaymentValidator paymentValidator;

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
                    amountResult.convertedAmount(),
                    amountResult.usedPointAmount()
            ).build();

            payment = paymentRepository.save(payment);
            loggingService.logPaymentRequest(payment);

            // 카카오페이의 경우 결제 준비 단계가 필요함 (preparePayment 호출)
            // preparePayment를 통해 tid와 next_redirect_pc_url을 받아와야 함
            if (request.getPaymentMethod() == PaymentMethod.KAKAO_PAY) {
                try {
                    PaymentProvider provider = providerFactory.getProvider(PaymentMethod.KAKAO_PAY);
                    
                    if (provider.requiresPreparation()) {
                        // 실제 결제 금액 계산 (포인트 사용 후 금액)
                        BigDecimal actualAmount = amountResult.convertedAmount().subtract(
                                amountResult.usedPointAmount() != null ? amountResult.usedPointAmount() : BigDecimal.ZERO
                        );
                        
                        // 상품명 추출 (metadata에서 가져오거나 기본값 사용)
                        String productName = extractProductName(request.getMetadata());
                        
                        var prepareResult = provider.preparePayment(
                                String.valueOf(payment.getId()),
                                actualAmount,
                                request.getCurrency(),
                                productName,
                                String.valueOf(userId)
                        );
                        
                        if (prepareResult.required() && prepareResult.redirectUrl() != null) {
                            // externalPaymentId(tid) 저장
                            payment.updateExternalPaymentId(prepareResult.tid());
                            
                            // metadata에 next_redirect_pc_url 추가
                            String updatedMetadata = addRedirectUrlToMetadata(
                                    request.getMetadata(),
                                    prepareResult.redirectUrl()
                            );
                            payment.updateMetadata(updatedMetadata);
                            
                            payment = paymentRepository.save(payment);
                            
                            log.info("카카오페이 결제 준비 완료: paymentId={}, tid={}, redirectUrl={}",
                                    payment.getId(), prepareResult.tid(), prepareResult.redirectUrl());
                        }
                    }
                } catch (Exception e) {
                    log.error("카카오페이 결제 준비 실패: paymentId={}, error={}",
                            payment.getId(), e.getMessage(), e);
                    throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR,
                            "카카오페이 결제 준비 실패: " + e.getMessage());
                }
            }

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

            try {
                postProcessService.processPaymentCancel(payment, userId, request.getReasonOrDefault(), oldStatus);
            } catch (Exception postProcessException) {
                // 후처리 실패는 로깅만 수행 (취소는 성공했으므로 예외를 던지지 않음)
                // TODO: 포인트 복구 실패 시 보상 트랜잭션 필요
                log.error("결제 취소 성공 후 후처리 실패: paymentId={}, userId={}, error={}",
                        payment.getId(), userId, postProcessException.getMessage(), postProcessException);
            }

        } catch (Exception e) {
            // 일관된 예외 처리: ApiException으로 래핑하여 throw
            log.error("결제 취소 실패: paymentId={}, userId={}, error={}", request.getPaymentId(), userId, e.getMessage(), e);
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, "결제 취소 실패: " + e.getMessage());
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

            try {
                postProcessService.processPaymentRefund(
                        payment,
                        userId,
                        refundAmount,
                        refundPointAmount,
                        request.getReasonOrDefault(),
                        oldStatus
                );
            } catch (Exception postProcessException) {
                // 후처리 실패는 로깅만 수행 (환불은 성공했으므로 예외를 던지지 않음)
                // TODO: 포인트 복구 실패 시 보상 트랜잭션 필요
                log.error("결제 환불 성공 후 후처리 실패: paymentId={}, userId={}, error={}",
                        payment.getId(), userId, postProcessException.getMessage(), postProcessException);
            }

        } catch (Exception e) {
            // 일관된 예외 처리: ApiException으로 래핑하여 throw
            log.error("결제 환불 실패: paymentId={}, userId={}, error={}", request.getPaymentId(), userId, e.getMessage(), e);
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, "결제 환불 실패: " + e.getMessage());
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

        // paymentKey 형식 검증
        paymentValidator.validatePaymentKey(request.getPaymentKey(), payment.getPaymentMethod());

        // 결제사별 paymentKey 처리
        // - 토스페이먼츠: 클라이언트에서 받은 paymentKey를 externalPaymentId로 설정
        // - 카카오페이: ready 시 받은 tid가 이미 externalPaymentId에 저장되어 있음
        if (request.getPaymentKey() != null && !request.getPaymentKey().isEmpty()) {
            // 카카오페이의 경우 ready 시 이미 tid가 저장되어 있으므로 업데이트하지 않음
            // 토스페이먼츠의 경우 클라이언트에서 받은 paymentKey를 설정
            if (payment.getPaymentMethod() != PaymentMethod.KAKAO_PAY) {
                payment.updateExternalPaymentId(request.getPaymentKey());
            }
        }

        // 실제 결제 금액 계산 (포인트 사용 후 금액)
        BigDecimal actualAmount = payment.getAmount().subtract(
                payment.getUsedPointAmount() != null ? payment.getUsedPointAmount() : BigDecimal.ZERO
        );

        // 카카오페이의 경우 pgToken을 additionalParams로 전달
        Map<String, String> additionalParams = Collections.emptyMap();
        if (payment.getPaymentMethod() == PaymentMethod.KAKAO_PAY) {
            paymentValidator.validateKakaoPayPgToken(request.getPgToken());
            additionalParams = Map.of("pgToken", request.getPgToken());
        }

        try {
            // PaymentExecutionService를 통한 결제 실행
            payment = executionService.executePayment(payment, actualAmount, additionalParams);

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
                // 후처리 실패는 로깅만 수행 (결제는 성공했으므로 예외를 던지지 않음)
                // TODO: 보상 트랜잭션 큐에 넣어 나중에 재시도하거나 관리자 알림 필요
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
                // 후처리 실패는 로깅만 수행
                log.error("결제 실패 후처리 중 오류 발생: paymentId={}, userId={}, error={}",
                        payment.getId(), userId, postProcessException.getMessage(), postProcessException);
            }

            // 일관된 예외 처리: ApiException으로 래핑하여 throw
            log.error("결제 승인 실패: paymentId={}, userId={}, error={}", payment.getId(), userId, e.getMessage(), e);
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, "결제 승인 실패: " + e.getMessage());
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

            try {
                postProcessService.processPaymentCancel(payment, payment.getUser().getId(), request.getReasonOrDefault(), oldStatus);
            } catch (Exception postProcessException) {
                // 후처리 실패는 로깅만 수행 (취소는 성공했으므로 예외를 던지지 않음)
                log.error("관리자 결제 취소 성공 후 후처리 실패: paymentId={}, adminId={}, error={}",
                        paymentId, adminId, postProcessException.getMessage(), postProcessException);
            }

        } catch (Exception e) {
            // 일관된 예외 처리: ApiException으로 래핑하여 throw
            log.error("관리자 결제 취소 실패: paymentId={}, adminId={}, error={}", paymentId, adminId, e.getMessage(), e);
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, "결제 취소 실패: " + e.getMessage());
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

            try {
                postProcessService.processPaymentRefund(
                        payment,
                        payment.getUser().getId(),
                        refundAmount,
                        refundPointAmount,
                        request.getReasonOrDefault(),
                        oldStatus
                );
            } catch (Exception postProcessException) {
                // 후처리 실패는 로깅만 수행 (환불은 성공했으므로 예외를 던지지 않음)
                log.error("관리자 결제 환불 성공 후 후처리 실패: paymentId={}, adminId={}, error={}",
                        paymentId, adminId, postProcessException.getMessage(), postProcessException);
            }

        } catch (Exception e) {
            // 일관된 예외 처리: ApiException으로 래핑하여 throw
            log.error("관리자 결제 환불 실패: paymentId={}, adminId={}, error={}", paymentId, adminId, e.getMessage(), e);
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, "결제 환불 실패: " + e.getMessage());
        }

        return PaymentResponseDto.from(payment);
    }

    /**
     * metadata에서 상품명 추출
     */
    private String extractProductName(String metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "상품";
        }
        
        try {
            Map<String, Object> metadataMap = objectMapper.readValue(
                    metadata,
                    new TypeReference<Map<String, Object>>() {}
            );
            Object productName = metadataMap.get("product_name");
            if (productName != null) {
                return productName.toString();
            }
        } catch (Exception e) {
            log.debug("metadata에서 상품명 추출 실패: {}", e.getMessage());
        }
        
        return "상품";
    }

    /**
     * metadata에 next_redirect_pc_url 추가
     */
    private String addRedirectUrlToMetadata(String existingMetadata, String redirectUrl) {
        try {
            Map<String, Object> metadataMap;
            
            if (existingMetadata != null && !existingMetadata.isEmpty()) {
                try {
                    metadataMap = objectMapper.readValue(
                            existingMetadata,
                            new TypeReference<Map<String, Object>>() {}
                    );
                } catch (Exception e) {
                    // 기존 metadata가 유효한 JSON이 아닌 경우 새로 생성
                    metadataMap = new java.util.HashMap<>();
                }
            } else {
                metadataMap = new java.util.HashMap<>();
            }
            
            // next_redirect_pc_url 추가
            metadataMap.put("next_redirect_pc_url", redirectUrl);
            metadataMap.put("redirect_url", redirectUrl); // 호환성을 위해 두 필드 모두 추가
            
            return objectMapper.writeValueAsString(metadataMap);
        } catch (Exception e) {
            log.error("metadata에 redirectUrl 추가 실패: {}", e.getMessage(), e);
            // 실패 시 기본 JSON 생성
            try {
                Map<String, String> fallbackMap = new java.util.HashMap<>();
                fallbackMap.put("next_redirect_pc_url", redirectUrl);
                fallbackMap.put("redirect_url", redirectUrl);
                return objectMapper.writeValueAsString(fallbackMap);
            } catch (Exception ex) {
                log.error("fallback metadata 생성 실패: {}", ex.getMessage());
                return "{\"next_redirect_pc_url\":\"" + redirectUrl + "\",\"redirect_url\":\"" + redirectUrl + "\"}";
            }
        }
    }

}

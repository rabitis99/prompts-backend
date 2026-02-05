package org.example.sharedprompts.domain.payment.application.command;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentUserType;
import org.example.sharedprompts.domain.payment.domain.enums.UserTier;
import org.example.sharedprompts.domain.payment.infrastructure.monitoring.PaymentLoggingService;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.PaymentProvider;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.PaymentProviderFactory;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.PaymentJpaAdapter;
import org.example.sharedprompts.domain.payment.application.dto.AmountProcessingResult;
import org.example.sharedprompts.domain.payment.domain.service.PaymentAmountCalculator;
import org.example.sharedprompts.domain.payment.domain.valueobject.Currency;
import org.example.sharedprompts.domain.payment.domain.valueobject.ExchangeRate;
import org.example.sharedprompts.domain.payment.domain.valueobject.PaymentAmount;
import org.example.sharedprompts.domain.payment.infrastructure.external.exchange.ExchangeRateService;
import org.example.sharedprompts.domain.payment.service.point.PointService;
import org.example.sharedprompts.domain.payment.application.command.PaymentExecutionService;
import org.example.sharedprompts.domain.payment.infrastructure.monitoring.compensation.CompensationQueue;
import org.example.sharedprompts.domain.payment.infrastructure.monitoring.compensation.CompensationTask;
import org.example.sharedprompts.domain.payment.infrastructure.monitoring.compensation.CompensationTaskType;
import org.example.sharedprompts.domain.payment.infrastructure.idempotency.IdempotencyService;
import org.example.sharedprompts.domain.payment.infrastructure.transaction.DistributedLockService;
import org.example.sharedprompts.domain.payment.application.command.postprocess.PaymentPostProcessService;
import org.example.sharedprompts.domain.payment.application.query.PaymentStatusSyncService;
import org.example.sharedprompts.domain.payment.infrastructure.transaction.PaymentTransactionManager;
import org.example.sharedprompts.domain.payment.application.command.PaymentValidationService;
import org.example.sharedprompts.domain.payment.domain.service.PaymentValidator;
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
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@Transactional(readOnly = true)
public class PaymentCommandServiceImpl implements PaymentCommandService {

    private final PaymentJpaAdapter paymentJpaAdapter;
    private final UserRepository userRepository;
    private final PaymentValidationService validationService;
    private final PaymentAmountCalculator amountCalculator;
    private final ExchangeRateService exchangeRateService;
    private final PointService pointService;
    private final PaymentExecutionService executionService;
    private final PaymentPostProcessService postProcessService;
    private final PaymentStatusSyncService statusSyncService;
    private final PaymentLoggingService loggingService;
    private final PaymentProviderFactory providerFactory;
    private final ObjectMapper objectMapper;
    private final PaymentValidator paymentValidator;
    private final DistributedLockService distributedLockService;
    private final PaymentTransactionManager transactionManager;
    private final CompensationQueue compensationQueue;
    private final IdempotencyService idempotencyService;

    public PaymentCommandServiceImpl(
            PaymentJpaAdapter paymentJpaAdapter,
            UserRepository userRepository,
            PaymentValidationService validationService,
            PaymentAmountCalculator amountCalculator,
            ExchangeRateService exchangeRateService,
            PointService pointService,
            PaymentExecutionService executionService,
            PaymentPostProcessService postProcessService,
            PaymentStatusSyncService statusSyncService,
            PaymentLoggingService loggingService,
            PaymentProviderFactory providerFactory,
            ObjectMapper objectMapper,
            PaymentValidator paymentValidator,
            DistributedLockService distributedLockService,
            PaymentTransactionManager transactionManager,
            CompensationQueue compensationQueue,
            IdempotencyService idempotencyService) {
        this.paymentJpaAdapter = paymentJpaAdapter;
        this.userRepository = userRepository;
        this.validationService = validationService;
        this.amountCalculator = amountCalculator;
        this.exchangeRateService = exchangeRateService;
        this.pointService = pointService;
        this.executionService = executionService;
        this.postProcessService = postProcessService;
        this.statusSyncService = statusSyncService;
        this.loggingService = loggingService;
        this.providerFactory = providerFactory;
        this.objectMapper = objectMapper;
        this.paymentValidator = paymentValidator;
        this.distributedLockService = distributedLockService;
        this.transactionManager = transactionManager;
        this.compensationQueue = compensationQueue;
        this.idempotencyService = idempotencyService;
    }

    @Override
    @Transactional
    public PaymentResponseDto requestPayment(Long userId, PaymentRequestDto request) {
        String traceId = null;

        try {
            traceId = loggingService.startTrace(null, userId);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

            validationService.validateDailyLimit(userId, user.getTier());

            AmountProcessingResult amountResult = processPaymentAmount(userId, request);

            Payment payment = request.toPaymentBuilder(
                    user,
                    amountResult.convertedAmount(),
                    amountResult.usedPointAmount()
            ).build();

            payment = paymentJpaAdapter.save(payment);
            loggingService.logPaymentRequest(payment);

            if (request.getPaymentMethod() == PaymentMethod.KAKAO_PAY) {
                try {
                    PaymentProvider provider = providerFactory.getProvider(PaymentMethod.KAKAO_PAY);
                    
                    if (provider.requiresPreparation()) {
                        BigDecimal actualAmount = amountResult.convertedAmount().subtract(
                                amountResult.usedPointAmount() != null ? amountResult.usedPointAmount() : BigDecimal.ZERO
                        );
                        
                        String productName = extractProductName(request.getMetadata());
                        
                        String idempotencyKey = idempotencyService.generateForPayment(payment);
                        payment.updateIdempotencyKey(idempotencyKey);
                        
                        var prepareResult = provider.preparePayment(
                                String.valueOf(payment.getId()),
                                actualAmount,
                                request.getCurrency(),
                                productName,
                                String.valueOf(userId),
                                idempotencyKey
                        );
                        
                        if (prepareResult.required() && prepareResult.redirectUrl() != null) {
                            payment.updateExternalPaymentId(prepareResult.tid());
                            
                            String updatedMetadata = addRedirectUrlToMetadata(
                                    request.getMetadata(),
                                    prepareResult.redirectUrl(),
                                    prepareResult.tid()
                            );
                            payment.updateMetadata(updatedMetadata);
                            
                            payment = paymentJpaAdapter.save(payment);
                            
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
        Payment payment = paymentJpaAdapter.findById(request.getPaymentIdAsLong())
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

        validationService.validatePaymentOwnership(payment, userId);
        validationService.validateCancelableStatus(payment);

        Payment canceledPayment;
        try {
            canceledPayment = executionService.executeCancel(payment, request.getReasonOrDefault());
        } catch (Exception e) {
            log.error("결제 취소 실패: paymentId={}, userId={}, error={}", request.getPaymentId(), userId, e.getMessage(), e);
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, "결제 취소 실패: " + e.getMessage());
        }

        try {
            postProcessService.processPaymentCancelAfterCommit(
                    canceledPayment.getId(),
                    userId,
                    request.getReasonOrDefault()
            );
        } catch (Exception postProcessException) {
            log.error("결제 취소 성공 후 후처리 실패: paymentId={}, userId={}, error={}",
                    canceledPayment.getId(), userId, postProcessException.getMessage(), postProcessException);
            
            CompensationTask task = new CompensationTask(
                    CompensationTaskType.POINT_RECOVERY_CANCEL,
                    canceledPayment.getId(),
                    userId,
                    canceledPayment.getUsedPointAmount(),
                    null,
                    postProcessException.getMessage(),
                    null
            );
            compensationQueue.enqueue(task);
        }

        return PaymentResponseDto.from(canceledPayment);
    }

    @Override
    public PaymentResponseDto refundPayment(Long userId, PaymentRefundRequestDto request) {
        Long paymentId = request.getPaymentIdAsLong();
        String lockKey = distributedLockService.createLockKey("payment", paymentId) + ":state";

        Payment refundedPayment = transactionManager.executeWithLockAndTransaction(lockKey, () -> {
            Payment payment = paymentJpaAdapter.findById(paymentId)
                    .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

            validationService.validatePaymentOwnership(payment, userId);
            validationService.validateRefundableStatus(payment);
            BigDecimal refundAmount = validationService.validateRefundAmount(request.getAmount(), payment);

            return executionService.executeRefund(payment, refundAmount, request.getReasonOrDefault());
        });

        BigDecimal refundAmount = validationService.validateRefundAmount(request.getAmount(), refundedPayment);
        BigDecimal refundPointAmount = amountCalculator.calculateRefundPointAmount(
                refundedPayment.getUsedPointAmount(),
                refundedPayment.getAmount(),
                refundAmount
        );

        try {
            postProcessService.processPaymentRefundAfterCommit(
                    refundedPayment.getId(),
                    userId,
                    refundAmount,
                    refundPointAmount,
                    request.getReasonOrDefault()
            );
        } catch (Exception postProcessException) {
            log.error("결제 환불 성공 후 후처리 실패: paymentId={}, userId={}, error={}",
                    refundedPayment.getId(), userId, postProcessException.getMessage(), postProcessException);
            
            CompensationTask task = new CompensationTask(
                    CompensationTaskType.POINT_RECOVERY_REFUND,
                    refundedPayment.getId(),
                    userId,
                    refundPointAmount,
                    null,
                    postProcessException.getMessage(),
                    null
            );
            compensationQueue.enqueue(task);
        }

        return PaymentResponseDto.from(refundedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponseDto> getPaymentHistory(Long userId, Pageable pageable) {
        return paymentJpaAdapter.findByUserIdWithFetchJoin(userId, pageable)
                .map(PaymentResponseDto::from);
    }

    @Override
    public PaymentConfirmResponse confirmPayment(Long userId, PaymentConfirmRequest request) {
        Long paymentId = request.getOrderIdAsLong();
        String lockKey = distributedLockService.createLockKey("payment", paymentId) + ":state";

        long[] processingTimeHolder = new long[1];
        boolean[] paymentSucceededHolder = new boolean[1];

        try {
            PaymentConfirmResponse response = transactionManager.executeWithLockAndTransaction(lockKey, () -> {
                long startTime = System.currentTimeMillis();

                Payment payment = paymentJpaAdapter.findByIdForUpdate(paymentId)
                        .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

                validationService.validatePaymentOwnership(payment, userId);

                if (payment.getStatus() == PaymentStatus.SUCCESS) {
                    return toConfirmResponse(payment, request);
                }

                paymentValidator.validatePaymentKey(request.getPaymentKey(), payment.getPaymentMethod());

                if (payment.getPaymentMethod() != PaymentMethod.KAKAO_PAY) {
                    if (request.getPaymentKey() == null || request.getPaymentKey().isEmpty()) {
                        throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "paymentKey",
                                "비카카오 결제는 paymentKey가 필수입니다");
                    }
                    payment.updateExternalPaymentId(request.getPaymentKey());
                }

                BigDecimal calculatedActualAmount = payment.getAmount().subtract(
                        payment.getUsedPointAmount() != null ? payment.getUsedPointAmount() : BigDecimal.ZERO
                );

                Map<String, String> additionalParams = new HashMap<>();
                if (payment.getPaymentMethod() == PaymentMethod.KAKAO_PAY) {
                    paymentValidator.validateKakaoPayPgToken(request.getPgToken());
                    additionalParams.put("pgToken", request.getPgToken());
                }
                if (payment.getPaymentMethod() == PaymentMethod.TOSS && request.getTossOrderId() != null && !request.getTossOrderId().isEmpty()) {
                    additionalParams.put("tossOrderId", request.getTossOrderId());
                    log.debug("Toss Payments orderId를 프론트엔드에서 받은 값으로 사용: tossOrderId={}, paymentId={}", 
                            request.getTossOrderId(), paymentId);
                }

                long calculatedProcessingTime;
                try {
                    payment = executionService.executePayment(payment, calculatedActualAmount, additionalParams);

                    calculatedProcessingTime = System.currentTimeMillis() - startTime;
                    processingTimeHolder[0] = calculatedProcessingTime;

                    loggingService.logPaymentApprovalSuccess(payment, payment.getExternalPaymentId(), calculatedProcessingTime);
                    loggingService.logPaymentStatusChange(payment, PaymentStatus.PENDING, PaymentStatus.SUCCESS);

                } catch (ObjectOptimisticLockingFailureException optimisticLockException) {
                    log.info("confirmPayment 낙관적 락 충돌(트랜잭션 본문), 최신 Payment 재조회: paymentId={}", paymentId);
                    Payment fresh = paymentJpaAdapter.findById(paymentId)
                            .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
                    if (fresh.getStatus() == PaymentStatus.SUCCESS) {
                        return toConfirmResponse(fresh, request);
                    }
                    throw optimisticLockException;

                } catch (Exception e) {
                    calculatedProcessingTime = System.currentTimeMillis() - startTime;
                    payment.fail("결제 승인 실패: " + e.getMessage());
                    payment = paymentJpaAdapter.save(payment);

                    try {
                        postProcessService.processPaymentFailure(
                                payment,
                                userId,
                                e.getMessage(),
                                e,
                                calculatedProcessingTime
                        );
                    } catch (Exception postProcessException) {
                        log.error("결제 실패 후처리 중 오류 발생: paymentId={}, userId={}, error={}",
                                payment.getId(), userId, postProcessException.getMessage(), postProcessException);
                    }

                    log.error("결제 승인 실패: paymentId={}, userId={}, error={}", payment.getId(), userId, e.getMessage(), e);
                    throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, "결제 승인 실패: " + e.getMessage());
                }

                PaymentConfirmResponse confirmResponse = toConfirmResponse(payment, request);
                paymentSucceededHolder[0] = true;
                return confirmResponse;
            });
            
            if (paymentSucceededHolder[0]) {
                Payment committedPayment = paymentJpaAdapter.findById(paymentId)
                        .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
                
                if (committedPayment.getStatus() == PaymentStatus.SUCCESS) {
                    BigDecimal actualAmount = committedPayment.getAmount().subtract(
                            committedPayment.getUsedPointAmount() != null ? committedPayment.getUsedPointAmount() : BigDecimal.ZERO
                    );
                    BigDecimal originalAmount = committedPayment.getAmount();
                    
                    try {
                        postProcessService.processPaymentSuccessAfterCommit(
                                paymentId,
                                userId,
                                actualAmount,
                                originalAmount,
                                processingTimeHolder[0]
                        );
                    } catch (Exception postProcessException) {
                        log.error("결제 승인 성공 후 후처리 실패: paymentId={}, userId={}, error={}",
                                paymentId, userId, postProcessException.getMessage(), postProcessException);
                        
                        CompensationTask task = new CompensationTask(
                                CompensationTaskType.POINT_ACCRUAL,
                                paymentId,
                                userId,
                                actualAmount,
                                null,
                                postProcessException.getMessage(),
                                null
                        );
                        compensationQueue.enqueue(task);
                    }
                }
            }
            
            return response;
        } catch (ObjectOptimisticLockingFailureException optimisticLockException) {
            log.info("confirmPayment 낙관적 락 충돌(커밋 시점), 최신 Payment 재조회: paymentId={}", paymentId);
            Payment fresh = paymentJpaAdapter.findById(paymentId)
                    .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
            if (fresh.getStatus() == PaymentStatus.SUCCESS) {
                return toConfirmResponse(fresh, request);
            }
            throw optimisticLockException;
        }
    }

    private PaymentConfirmResponse toConfirmResponse(Payment payment, PaymentConfirmRequest request) {
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

    private String addRedirectUrlToMetadata(String existingMetadata, String redirectUrl, String tid) {
        try {
            Map<String, Object> metadataMap;
            
            if (existingMetadata != null && !existingMetadata.isEmpty()) {
                try {
                    metadataMap = objectMapper.readValue(
                            existingMetadata,
                            new TypeReference<Map<String, Object>>() {}
                    );
                } catch (Exception e) {
                    metadataMap = new java.util.HashMap<>();
                }
            } else {
                metadataMap = new java.util.HashMap<>();
            }
            
            metadataMap.put("next_redirect_pc_url", redirectUrl);
            metadataMap.put("redirect_url", redirectUrl);
            if (tid != null && !tid.isEmpty()) {
                metadataMap.put("tid", tid);
            }
            
            return objectMapper.writeValueAsString(metadataMap);
        } catch (Exception e) {
            log.error("metadata에 redirectUrl 추가 실패: {}", e.getMessage(), e);
            try {
                Map<String, String> fallbackMap = new java.util.HashMap<>();
                fallbackMap.put("next_redirect_pc_url", redirectUrl);
                fallbackMap.put("redirect_url", redirectUrl);
                if (tid != null && !tid.isEmpty()) {
                    fallbackMap.put("tid", tid);
                }
                return objectMapper.writeValueAsString(fallbackMap);
            } catch (Exception ex) {
                log.error("fallback metadata 생성 실패", ex);
                log.error("redirectUrl을 metadata에 추가할 수 없습니다: {}", redirectUrl);
                return "{}";
            }
        }
    }

    @Transactional
    public void updatePaymentUserTypeAndTier(Long paymentId, PaymentUserType userType, UserTier tier) {
        Payment payment = paymentJpaAdapter.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE,
                    String.format("결제 상태가 SUCCESS가 아니면 userType과 tier를 업데이트할 수 없습니다. 현재 상태: %s", payment.getStatus()));
        }

        payment.updateUserTypeAndTier(userType, tier);
        paymentJpaAdapter.save(payment);

        log.info("결제 userType 및 tier 업데이트 완료: paymentId={}, userType={}, tier={}", 
                paymentId, userType, tier);
    }

    private AmountProcessingResult processPaymentAmount(Long userId, PaymentRequestDto request) {
        PaymentAmount originalPaymentAmount = PaymentAmount.of(
            request.getAmount(),
            request.getCurrency()
        );
        
        PaymentAmount convertedPaymentAmount = originalPaymentAmount;
        if (!originalPaymentAmount.getCurrency().equals(Currency.KRW())) {
            BigDecimal exchangeRate = exchangeRateService.getExchangeRate(
                originalPaymentAmount.getCurrencyCode(),
                Currency.KRW().getCode()
            );
            
            ExchangeRate rate = ExchangeRate.of(
                originalPaymentAmount.getCurrency(),
                Currency.KRW(),
                exchangeRate
            );
            
            convertedPaymentAmount = amountCalculator.convertCurrency(originalPaymentAmount, rate);
            log.debug("환율 변환: {} -> {}", originalPaymentAmount, convertedPaymentAmount);
        }

        PaymentAmount usePointPaymentAmount = request.getUsePointAmount() != null 
            ? PaymentAmount.krw(request.getUsePointAmount())
            : PaymentAmount.krw(BigDecimal.ZERO);
        
        PaymentAmount actualPaymentAmount = convertedPaymentAmount;
        
        if (usePointPaymentAmount.isPositive()) {
            BigDecimal currentBalance = pointService.getCurrentBalance(userId);
            PaymentAmount currentBalanceAmount = PaymentAmount.krw(currentBalance);
            
            if (currentBalanceAmount.isLessThan(usePointPaymentAmount)) {
                throw new ApiException(ErrorCode.POINT_INSUFFICIENT);
            }
            
            if (usePointPaymentAmount.isGreaterThan(convertedPaymentAmount)) {
                usePointPaymentAmount = convertedPaymentAmount;
            }
            
            pointService.usePoints(userId, usePointPaymentAmount.toBigDecimal(), "결제 시 포인트 사용");
            actualPaymentAmount = amountCalculator.calculateActualAmount(convertedPaymentAmount, usePointPaymentAmount);
            
            log.info("포인트 사용: userId={}, usePointAmount={}, originalAmount={}, actualPaymentAmount={}", 
                    userId, usePointPaymentAmount, convertedPaymentAmount, actualPaymentAmount);
        }

        return new AmountProcessingResult(
            originalPaymentAmount.toBigDecimal(),
            convertedPaymentAmount.toBigDecimal(),
            usePointPaymentAmount.toBigDecimal(),
            actualPaymentAmount.toBigDecimal()
        );
    }

}


package org.example.sharedprompts.domain.payment.service.core;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.enums.PaymentUserType;
import org.example.sharedprompts.domain.payment.enums.UserTier;
import org.example.sharedprompts.domain.payment.logging.PaymentLoggingService;
import org.example.sharedprompts.domain.payment.provider.PaymentProvider;
import org.example.sharedprompts.domain.payment.provider.PaymentProviderFactory;
import org.example.sharedprompts.domain.payment.repository.payment.PaymentRepository;
import org.example.sharedprompts.domain.payment.service.facade.AmountProcessingResult;
import org.example.sharedprompts.domain.payment.service.facade.PaymentAmountFacade;
import org.example.sharedprompts.domain.payment.service.execution.PaymentExecutionService;
import org.example.sharedprompts.domain.payment.service.lock.DistributedLockService;
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
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

/**
 * 결제 서비스 구현체
 * 파사드 패턴을 사용하여 복잡한 로직을 분리
 */
@Slf4j
@Service
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
    private final DistributedLockService distributedLockService;
    private final TransactionTemplate transactionTemplate;

    /**
     * REQUIRES_NEW 전파로 설정된 TransactionTemplate을 생성합니다.
     * 상위 트랜잭션과 독립적으로 실행되어 락 획득 → 트랜잭션 시작 순서를 보장합니다.
     */
    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            UserRepository userRepository,
            PaymentValidationService validationService,
            PaymentAmountFacade amountFacade,
            PaymentExecutionService executionService,
            PaymentPostProcessService postProcessService,
            PaymentStatusSyncService statusSyncService,
            PaymentLoggingService loggingService,
            PaymentProviderFactory providerFactory,
            ObjectMapper objectMapper,
            PaymentValidator paymentValidator,
            DistributedLockService distributedLockService,
            PlatformTransactionManager transactionManager) {
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.validationService = validationService;
        this.amountFacade = amountFacade;
        this.executionService = executionService;
        this.postProcessService = postProcessService;
        this.statusSyncService = statusSyncService;
        this.loggingService = loggingService;
        this.providerFactory = providerFactory;
        this.objectMapper = objectMapper;
        this.paymentValidator = paymentValidator;
        this.distributedLockService = distributedLockService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
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

            AmountProcessingResult amountResult =
                    amountFacade.processPaymentAmount(userId, request);

            Payment payment = request.toPaymentBuilder(
                    user,
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
                            
                            // metadata에 next_redirect_pc_url 및 tid 추가
                            String updatedMetadata = addRedirectUrlToMetadata(
                                    request.getMetadata(),
                                    prepareResult.redirectUrl(),
                                    prepareResult.tid()
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
                // TODO: 보상 트랜잭션 큐에 추가하여 나중에 재시도하거나 관리자 알림 필요
                // 포인트 복구 실패 시 별도 보상 처리 프로세스 필요
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

    /**
     * 결제 환불 처리
     *
     * <p><strong>동시성 보호 (2026-02-04 개선):</strong>
     * 분산 락을 사용하여 동일 Payment에 대한 동시 환불 요청을 직렬화합니다.
     * 이를 통해 멱등성 키 생성 시 refundedAmount 읽기 경쟁 조건을 방지합니다.
     *
     * <p><strong>트랜잭션 순서 (2026-02-04 개선):</strong>
     * 락 획득 → 트랜잭션 시작 → 작업 수행 → 트랜잭션 커밋 → 락 해제
     * 이를 통해 락이 해제된 후 트랜잭션이 커밋되기 전에 다른 스레드가 락을 획득하는 문제를 방지합니다.
     */
    @Override
    public PaymentResponseDto refundPayment(Long userId, PaymentRefundRequestDto request) {
        Long paymentId = request.getPaymentIdAsLong();
        String lockKey = distributedLockService.createLockKey("payment", paymentId) + ":refund";

        return distributedLockService.executeWithLock(lockKey, () -> {
            // 락 내에서 트랜잭션 실행
            return transactionTemplate.execute(status -> {
                Payment payment = paymentRepository.findById(paymentId)
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
                        // TODO: 보상 트랜잭션 큐에 추가하여 나중에 재시도하거나 관리자 알림 필요
                        // 포인트 복구 실패 시 별도 보상 처리 프로세스 필요
                        log.error("결제 환불 성공 후 후처리 실패: paymentId={}, userId={}, error={}",
                                payment.getId(), userId, postProcessException.getMessage(), postProcessException);
                    }

                } catch (Exception e) {
                    // 일관된 예외 처리: ApiException으로 래핑하여 throw
                    log.error("결제 환불 실패: paymentId={}, userId={}, error={}", paymentId, userId, e.getMessage(), e);
                    throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, "결제 환불 실패: " + e.getMessage());
                }

                return PaymentResponseDto.from(payment);
            });
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponseDto> getPaymentHistory(Long userId, Pageable pageable) {
        return paymentRepository.findByUserIdWithFetchJoin(userId, pageable)
                .map(PaymentResponseDto::from);
    }

    @Override
    public PaymentConfirmResponse confirmPayment(Long userId, PaymentConfirmRequest request) {
        Long paymentId = request.getOrderIdAsLong();
        // confirm/webhook/cancel/refund 등 Payment 상태 변경은 동일 키로 직렬화해야 충돌이 줄어듭니다.
        String lockKey = distributedLockService.createLockKey("payment", paymentId) + ":state";

        // 포인트 적립을 위한 정보를 저장할 변수 (배열을 사용하여 람다 내부에서 수정 가능하도록)
        long[] processingTimeHolder = new long[1];
        boolean[] paymentSucceededHolder = new boolean[1];

        try {
            PaymentConfirmResponse response = distributedLockService.executeWithLock(lockKey, () -> transactionTemplate.execute(txStatus -> {
                long startTime = System.currentTimeMillis();

                Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                        .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

                validationService.validatePaymentOwnership(payment, userId);

                // 이미 SUCCESS면 멱등 응답 (중복 confirm 호출 대비)
                if (payment.getStatus() == PaymentStatus.SUCCESS) {
                    return toConfirmResponse(payment, request);
                }

                // paymentKey 형식 검증
                paymentValidator.validatePaymentKey(request.getPaymentKey(), payment.getPaymentMethod());

                // 결제사별 paymentKey 처리
                // - 토스페이먼츠: 클라이언트에서 받은 paymentKey를 externalPaymentId로 설정
                // - 카카오페이: ready 시 받은 tid가 이미 externalPaymentId에 저장되어 있음
                if (payment.getPaymentMethod() != PaymentMethod.KAKAO_PAY) {
                    if (request.getPaymentKey() == null || request.getPaymentKey().isEmpty()) {
                        throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "paymentKey",
                                "비카카오 결제는 paymentKey가 필수입니다");
                    }
                    payment.updateExternalPaymentId(request.getPaymentKey());
                }

                // 실제 결제 금액 계산 (포인트 사용 후 금액)
                BigDecimal calculatedActualAmount = payment.getAmount().subtract(
                        payment.getUsedPointAmount() != null ? payment.getUsedPointAmount() : BigDecimal.ZERO
                );

                // 카카오페이의 경우 pgToken을 additionalParams로 전달
                Map<String, String> additionalParams = Collections.emptyMap();
                if (payment.getPaymentMethod() == PaymentMethod.KAKAO_PAY) {
                    paymentValidator.validateKakaoPayPgToken(request.getPgToken());
                    additionalParams = Map.of("pgToken", request.getPgToken());
                }

                long calculatedProcessingTime;
                try {
                    // PaymentExecutionService를 통한 결제 실행
                    payment = executionService.executePayment(payment, calculatedActualAmount, additionalParams);

                    calculatedProcessingTime = System.currentTimeMillis() - startTime;
                    processingTimeHolder[0] = calculatedProcessingTime;

                    // 로깅만 트랜잭션 내에서 수행 (빠른 커밋을 위해)
                    loggingService.logPaymentApprovalSuccess(payment, payment.getExternalPaymentId(), calculatedProcessingTime);
                    loggingService.logPaymentStatusChange(payment, PaymentStatus.PENDING, PaymentStatus.SUCCESS);

                } catch (ObjectOptimisticLockingFailureException optimisticLockException) {
                    // 트랜잭션 본문 내에서 발생한 낙관락 충돌은 여기서 처리 가능
                    log.info("confirmPayment 낙관적 락 충돌(트랜잭션 본문), 최신 Payment 재조회: paymentId={}", paymentId);
                    Payment fresh = paymentRepository.findById(paymentId)
                            .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
                    if (fresh.getStatus() == PaymentStatus.SUCCESS) {
                        return toConfirmResponse(fresh, request);
                    }
                    throw optimisticLockException;

                } catch (Exception e) {
                    calculatedProcessingTime = System.currentTimeMillis() - startTime;
                    payment.fail("결제 승인 실패: " + e.getMessage());
                    payment = paymentRepository.save(payment);

                    try {
                        postProcessService.processPaymentFailure(
                                payment,
                                userId,
                                e.getMessage(),
                                e,
                                calculatedProcessingTime
                        );
                    } catch (Exception postProcessException) {
                        // 후처리 실패는 로깅만 수행
                        log.error("결제 실패 후처리 중 오류 발생: paymentId={}, userId={}, error={}",
                                payment.getId(), userId, postProcessException.getMessage(), postProcessException);
                    }

                    log.error("결제 승인 실패: paymentId={}, userId={}, error={}", payment.getId(), userId, e.getMessage(), e);
                    throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, "결제 승인 실패: " + e.getMessage());
                }

                PaymentConfirmResponse confirmResponse = toConfirmResponse(payment, request);
                paymentSucceededHolder[0] = true;
                return confirmResponse;
            }));
            
            // 락이 완전히 해제된 후 포인트 적립 처리
            // 트랜잭션이 커밋되고 락이 해제된 후 별도 트랜잭션에서 포인트 적립을 처리합니다.
            // 이를 통해 락 타임아웃 문제를 방지합니다.
            if (paymentSucceededHolder[0]) {
                // Payment 엔티티를 다시 조회하여 최신 정보를 가져옵니다.
                Payment committedPayment = paymentRepository.findById(paymentId)
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
                        // 후처리 실패는 로깅만 수행 (결제는 성공했으므로 예외를 던지지 않음)
                        log.error("결제 승인 성공 후 후처리 실패: paymentId={}, userId={}, error={}",
                                paymentId, userId, postProcessException.getMessage(), postProcessException);
                    }
                }
            }
            
            return response;
        } catch (ObjectOptimisticLockingFailureException optimisticLockException) {
            // 커밋/flush 시점에 발생한 낙관락 충돌은 transactionTemplate.execute 바깥에서만 잡을 수 있음
            log.info("confirmPayment 낙관적 락 충돌(커밋 시점), 최신 Payment 재조회: paymentId={}", paymentId);
            Payment fresh = paymentRepository.findById(paymentId)
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

    /**
     * 관리자용 결제 환불 처리
     *
     * <p><strong>동시성 보호 (2026-02-04 개선):</strong>
     * 분산 락을 사용하여 동일 Payment에 대한 동시 환불 요청을 직렬화합니다.
     * 이를 통해 멱등성 키 생성 시 refundedAmount 읽기 경쟁 조건을 방지합니다.
     *
     * <p><strong>트랜잭션 순서 (2026-02-04 개선):</strong>
     * 락 획득 → 트랜잭션 시작 → 작업 수행 → 트랜잭션 커밋 → 락 해제
     * 이를 통해 락이 해제된 후 트랜잭션이 커밋되기 전에 다른 스레드가 락을 획득하는 문제를 방지합니다.
     */
    @Override
    public PaymentResponseDto refundPaymentForAdmin(Long paymentId, PaymentRefundRequestDto request, Long adminId) {
        String lockKey = distributedLockService.createLockKey("payment", paymentId) + ":refund";

        return distributedLockService.executeWithLock(lockKey, () -> {
            // 락 내에서 트랜잭션 실행
            return transactionTemplate.execute(status -> {
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
            });
        });
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
                    // 기존 metadata가 유효한 JSON이 아닌 경우 새로 생성
                    metadataMap = new java.util.HashMap<>();
                }
            } else {
                metadataMap = new java.util.HashMap<>();
            }
            
            // next_redirect_pc_url 추가
            metadataMap.put("next_redirect_pc_url", redirectUrl);
            metadataMap.put("redirect_url", redirectUrl); // 호환성을 위해 두 필드 모두 추가
            // 프론트에서 tid 복원을 쉽게 하기 위해 같이 저장
            if (tid != null && !tid.isEmpty()) {
                metadataMap.put("tid", tid);
            }
            
            return objectMapper.writeValueAsString(metadataMap);
        } catch (Exception e) {
            log.error("metadata에 redirectUrl 추가 실패: {}", e.getMessage(), e);
            // 실패 시 기본 JSON 생성
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
                // ObjectMapper 실패 시 안전한 기본값 반환 (URL에 특수문자가 있을 수 있으므로)
                log.error("redirectUrl을 metadata에 추가할 수 없습니다: {}", redirectUrl);
                return "{}";
            }
        }
    }

    /**
     * 결제의 사용자 타입 및 티어 업데이트
     *
     * <p><strong>주의:</strong>
     * 결제가 SUCCESS 상태일 때만 userType과 tier를 업데이트할 수 있습니다.
     * 결제 실패 시 등급 상승을 방지하기 위한 보호 메커니즘입니다.
     *
     * @param paymentId 결제 ID
     * @param userType 사용자 타입
     * @param tier 사용자 티어
     * @throws ApiException 결제 상태가 SUCCESS가 아닐 때
     */
    @Transactional
    public void updatePaymentUserTypeAndTier(Long paymentId, PaymentUserType userType, UserTier tier) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

        // 결제 상태 검증: SUCCESS 상태일 때만 업데이트 가능
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE,
                    String.format("결제 상태가 SUCCESS가 아니면 userType과 tier를 업데이트할 수 없습니다. 현재 상태: %s", payment.getStatus()));
        }

        // 엔티티의 메서드를 통해 업데이트 (검증은 서비스 레이어에서 수행)
        payment.updateUserTypeAndTier(userType, tier);
        paymentRepository.save(payment);

        log.info("결제 userType 및 tier 업데이트 완료: paymentId={}, userType={}, tier={}", 
                paymentId, userType, tier);
    }

}

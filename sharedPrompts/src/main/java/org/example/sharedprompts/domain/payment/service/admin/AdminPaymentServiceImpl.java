package org.example.sharedprompts.domain.payment.service.admin;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.repository.payment.PaymentRepository;
import org.example.sharedprompts.domain.payment.service.facade.PaymentAmountFacade;
import org.example.sharedprompts.domain.payment.service.execution.PaymentExecutionService;
import org.example.sharedprompts.domain.payment.service.compensation.CompensationQueue;
import org.example.sharedprompts.domain.payment.service.compensation.CompensationTask;
import org.example.sharedprompts.domain.payment.service.compensation.CompensationTaskType;
import org.example.sharedprompts.domain.payment.service.lock.DistributedLockService;
import org.example.sharedprompts.domain.payment.service.postprocess.PaymentPostProcessService;
import org.example.sharedprompts.domain.payment.service.sync.PaymentStatusSyncService;
import org.example.sharedprompts.domain.payment.service.transaction.PaymentTransactionBoundary;
import org.example.sharedprompts.domain.payment.service.validation.PaymentValidationService;
import org.example.sharedprompts.dto.payment.request.PaymentCancelRequestDto;
import org.example.sharedprompts.dto.payment.request.PaymentRefundRequestDto;
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
 * 관리자용 결제 서비스 구현체
 * 
 * <p>관리자 전용 기능을 제공합니다:
 * <ul>
 *   <li>소유권 검증 없이 모든 결제 조회/취소/환불 가능</li>
 *   <li>전체 결제 내역 조회</li>
 * </ul>
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class AdminPaymentServiceImpl implements AdminPaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentValidationService validationService;
    private final PaymentAmountFacade amountFacade;
    private final PaymentExecutionService executionService;
    private final PaymentPostProcessService postProcessService;
    private final PaymentStatusSyncService statusSyncService;
    private final DistributedLockService distributedLockService;
    private final PaymentTransactionBoundary transactionBoundary;
    private final CompensationQueue compensationQueue;

    public AdminPaymentServiceImpl(
            PaymentRepository paymentRepository,
            PaymentValidationService validationService,
            PaymentAmountFacade amountFacade,
            PaymentExecutionService executionService,
            PaymentPostProcessService postProcessService,
            PaymentStatusSyncService statusSyncService,
            DistributedLockService distributedLockService,
            PaymentTransactionBoundary transactionBoundary,
            CompensationQueue compensationQueue) {
        this.paymentRepository = paymentRepository;
        this.validationService = validationService;
        this.amountFacade = amountFacade;
        this.executionService = executionService;
        this.postProcessService = postProcessService;
        this.statusSyncService = statusSyncService;
        this.distributedLockService = distributedLockService;
        this.transactionBoundary = transactionBoundary;
        this.compensationQueue = compensationQueue;
    }

    @Override
    @Transactional
    public PaymentStatusResponseDto checkPaymentStatus(Long paymentId) {
        return statusSyncService.syncPaymentStatusForAdmin(paymentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponseDto> getAllPaymentHistory(Pageable pageable) {
        return paymentRepository.findAllWithFetchJoin(pageable)
                .map(PaymentResponseDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponseDto> getPaymentHistory(Long userId, Pageable pageable) {
        return paymentRepository.findByUserIdWithFetchJoin(userId, pageable)
                .map(PaymentResponseDto::from);
    }

    /**
     * 관리자용 결제 취소 처리
     *
     * <p><strong>동시성 보호:</strong>
     * 분산 락을 사용하여 동일 Payment에 대한 동시 취소 요청을 직렬화합니다.
     * 이를 통해 전액 취소 시 동시성 문제를 방지합니다.
     *
     * <p><strong>트랜잭션 순서:</strong>
     * 락 획득 → 트랜잭션 시작 → 작업 수행 → 트랜잭션 커밋 → 락 해제
     * 이를 통해 락이 해제된 후 트랜잭션이 커밋되기 전에 다른 스레드가 락을 획득하는 문제를 방지합니다.
     */
    @Override
    public PaymentResponseDto cancelPayment(Long paymentId, PaymentCancelRequestDto request, Long adminId) {
        String lockKey = distributedLockService.createLockKey("payment", paymentId) + ":state";

        // 취소 실행 트랜잭션
        Payment canceledPayment;
        try {
            canceledPayment = transactionBoundary.executeWithLockAndTransaction(lockKey, () -> {
                Payment payment = paymentRepository.findById(paymentId)
                        .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

                // 관리자는 소유권 검증 없이 취소 가능
                validationService.validateCancelableStatus(payment);

                // PaymentExecutionService를 통한 취소 실행
                return executionService.executeCancel(payment, request.getReasonOrDefault());
            });
        } catch (Exception e) {
            // 일관된 예외 처리: ApiException으로 래핑하여 throw
            log.error("관리자 결제 취소 실패: paymentId={}, adminId={}, error={}", paymentId, adminId, e.getMessage(), e);
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, "결제 취소 실패: " + e.getMessage());
        }

        // 후처리 트랜잭션 (별도)
        try {
            postProcessService.processPaymentCancelAfterCommit(
                    canceledPayment.getId(),
                    canceledPayment.getUser().getId(),
                    request.getReasonOrDefault()
            );
        } catch (Exception postProcessException) {
            // 후처리 실패는 보상 트랜잭션 큐에 추가하여 나중에 재시도
            log.error("관리자 결제 취소 성공 후 후처리 실패: paymentId={}, adminId={}, error={}",
                    paymentId, adminId, postProcessException.getMessage(), postProcessException);
            
            CompensationTask task = new CompensationTask(
                    CompensationTaskType.POINT_RECOVERY_CANCEL,
                    canceledPayment.getId(),
                    canceledPayment.getUser().getId(),
                    canceledPayment.getUsedPointAmount(),
                    null,
                    postProcessException.getMessage(),
                    null
            );
            compensationQueue.enqueue(task);
        }

        return PaymentResponseDto.from(canceledPayment);
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
    public PaymentResponseDto refundPayment(Long paymentId, PaymentRefundRequestDto request, Long adminId) {
        String lockKey = distributedLockService.createLockKey("payment", paymentId) + ":state";

        // 환불 실행 트랜잭션
        Payment refundedPayment = transactionBoundary.executeWithLockAndTransaction(lockKey, () -> {
            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

            // 관리자는 소유권 검증 없이 환불 가능
            validationService.validateRefundableStatus(payment);
            BigDecimal refundAmount = validationService.validateRefundAmount(request.getAmount(), payment);

            // PaymentExecutionService를 통한 환불 실행
            return executionService.executeRefund(payment, refundAmount, request.getReasonOrDefault());
        });

        // 후처리 트랜잭션 (별도)
        BigDecimal refundAmount = validationService.validateRefundAmount(request.getAmount(), refundedPayment);
        BigDecimal refundPointAmount = amountFacade.calculateRefundPointAmount(
                refundedPayment.getUsedPointAmount(),
                refundedPayment.getAmount(),
                refundAmount
        );

        try {
            postProcessService.processPaymentRefundAfterCommit(
                    refundedPayment.getId(),
                    refundedPayment.getUser().getId(),
                    refundAmount,
                    refundPointAmount,
                    request.getReasonOrDefault()
            );
        } catch (Exception postProcessException) {
            // 후처리 실패는 보상 트랜잭션 큐에 추가하여 나중에 재시도
            log.error("관리자 결제 환불 성공 후 후처리 실패: paymentId={}, adminId={}, error={}",
                    paymentId, adminId, postProcessException.getMessage(), postProcessException);
            
            CompensationTask task = new CompensationTask(
                    CompensationTaskType.POINT_RECOVERY_REFUND,
                    refundedPayment.getId(),
                    refundedPayment.getUser().getId(),
                    refundPointAmount,
                    null,
                    postProcessException.getMessage(),
                    null
            );
            compensationQueue.enqueue(task);
        }

        return PaymentResponseDto.from(refundedPayment);
    }
}


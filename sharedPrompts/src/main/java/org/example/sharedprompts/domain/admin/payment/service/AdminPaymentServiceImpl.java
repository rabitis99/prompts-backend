package org.example.sharedprompts.domain.admin.payment.service;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.PaymentJpaAdapter;
import org.example.sharedprompts.domain.payment.domain.service.PaymentAmountCalculator;
import org.example.sharedprompts.domain.payment.application.command.PaymentExecutionService;
import org.example.sharedprompts.domain.payment.application.command.orchestrator.CompensationHandler;
import org.example.sharedprompts.domain.payment.infrastructure.monitoring.compensation.CompensationTaskType;
import org.example.sharedprompts.domain.payment.infrastructure.transaction.DistributedLockService;
import org.example.sharedprompts.domain.payment.application.command.postprocess.PaymentPostProcessService;
import org.example.sharedprompts.domain.payment.application.query.PaymentStatusSyncService;
import org.example.sharedprompts.domain.payment.application.command.service.refund.RefundExecutionResult;
import org.example.sharedprompts.domain.payment.infrastructure.transaction.PaymentTransactionManager;
import org.example.sharedprompts.domain.payment.application.command.PaymentValidationService;
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

@Slf4j
@Service
@Transactional(readOnly = true)
public class AdminPaymentServiceImpl implements AdminPaymentService {

    private final PaymentJpaAdapter paymentJpaAdapter;
    private final PaymentValidationService validationService;
    private final PaymentAmountCalculator amountCalculator;
    private final PaymentExecutionService executionService;
    private final PaymentPostProcessService postProcessService;
    private final PaymentStatusSyncService statusSyncService;
    private final DistributedLockService distributedLockService;
    private final PaymentTransactionManager transactionManager;
    private final CompensationHandler compensationHandler;

    public AdminPaymentServiceImpl(
            PaymentJpaAdapter paymentJpaAdapter,
            PaymentValidationService validationService,
            PaymentAmountCalculator amountCalculator,
            PaymentExecutionService executionService,
            PaymentPostProcessService postProcessService,
            PaymentStatusSyncService statusSyncService,
            DistributedLockService distributedLockService,
            PaymentTransactionManager transactionManager,
            CompensationHandler compensationHandler) {
        this.paymentJpaAdapter = paymentJpaAdapter;
        this.validationService = validationService;
        this.amountCalculator = amountCalculator;
        this.executionService = executionService;
        this.postProcessService = postProcessService;
        this.statusSyncService = statusSyncService;
        this.distributedLockService = distributedLockService;
        this.transactionManager = transactionManager;
        this.compensationHandler = compensationHandler;
    }

    @Override
    @Transactional
    public PaymentStatusResponseDto checkPaymentStatus(Long paymentId) {
        return statusSyncService.syncPaymentStatusForAdmin(paymentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponseDto> getAllPaymentHistory(Pageable pageable) {
        return paymentJpaAdapter.findAllWithFetchJoin(pageable)
                .map(PaymentResponseDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponseDto> getPaymentHistory(Long userId, Pageable pageable) {
        return paymentJpaAdapter.findByUserIdWithFetchJoin(userId, pageable)
                .map(PaymentResponseDto::from);
    }

    @Override
    public PaymentResponseDto cancelPayment(Long paymentId, PaymentCancelRequestDto request, Long adminId) {
        String lockKey = distributedLockService.createLockKey("payment", paymentId) + ":state";

        Payment canceledPayment;
        try {
            canceledPayment = transactionManager.executeWithLockAndTransaction(lockKey, () -> {
                Payment payment = paymentJpaAdapter.findByIdWithFetchJoin(paymentId)
                        .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

                validationService.validateCancelableStatus(payment);

                return executionService.executeCancel(payment, request.getReasonOrDefault());
            });
        } catch (Exception e) {
            throw handleTransactionFailure(e, "관리자 결제 취소 실패", paymentId, adminId);
        }

        try {
            postProcessService.processPaymentCancelAfterCommit(
                    canceledPayment.getId(),
                    canceledPayment.getUser().getId(),
                    request.getReasonOrDefault()
            );
        } catch (Exception postProcessException) {
            log.error("관리자 결제 취소 성공 후 후처리 실패: paymentId={}, adminId={}, error={}",
                    paymentId, adminId, postProcessException.getMessage(), postProcessException);
            
            compensationHandler.handlePostProcessFailure(
                    CompensationTaskType.POINT_RECOVERY_CANCEL,
                    canceledPayment.getId(),
                    canceledPayment.getUser().getId(),
                    canceledPayment.getUsedPointAmount(),
                    postProcessException.getMessage()
            );
        }

        return PaymentResponseDto.from(canceledPayment);
    }

    @Override
    public PaymentResponseDto refundPayment(Long paymentId, PaymentRefundRequestDto request, Long adminId) {
        String lockKey = distributedLockService.createLockKey("payment", paymentId) + ":state";

        RefundExecutionResult result;
        try {
            result = transactionManager.executeWithLockAndTransaction(lockKey, () -> {
                Payment payment = paymentJpaAdapter.findByIdWithFetchJoin(paymentId)
                        .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

                validationService.validateRefundableStatus(payment);
                BigDecimal refundAmount = validationService.validateRefundAmount(request.getAmount(), payment);

                Payment refundedPayment = executionService.executeRefund(payment, refundAmount, request.getReasonOrDefault());
                return new RefundExecutionResult(refundedPayment, refundAmount);
            });
        } catch (Exception e) {
            throw handleTransactionFailure(e, "관리자 결제 환불 실패", paymentId, adminId);
        }

        Payment refundedPayment = result.getRefundedPayment();
        BigDecimal refundAmount = result.getRefundAmount();
        BigDecimal refundPointAmount = amountCalculator.calculateRefundPointAmount(
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
            log.error("관리자 결제 환불 성공 후 후처리 실패: paymentId={}, adminId={}, error={}",
                    paymentId, adminId, postProcessException.getMessage(), postProcessException);
            
            compensationHandler.handlePostProcessFailure(
                    CompensationTaskType.POINT_RECOVERY_REFUND,
                    refundedPayment.getId(),
                    refundedPayment.getUser().getId(),
                    refundPointAmount,
                    postProcessException.getMessage()
            );
        }

        return PaymentResponseDto.from(refundedPayment);
    }

    private RuntimeException handleTransactionFailure(Exception e, String context, Long paymentId, Long actorId) {
        if (e instanceof ApiException apiEx) {
            throw apiEx;
        }
        log.error("{}: paymentId={}, actorId={}, error={}", context, paymentId, actorId, e.getMessage(), e);
        throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, context + ": " + e.getMessage(), e);
    }
}
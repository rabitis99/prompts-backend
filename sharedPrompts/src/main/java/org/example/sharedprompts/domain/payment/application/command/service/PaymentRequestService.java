package org.example.sharedprompts.domain.payment.application.command.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.application.command.PaymentValidationService;
import org.example.sharedprompts.domain.payment.application.command.service.amount.PaymentAmountProcessingService;
import org.example.sharedprompts.domain.payment.application.command.service.request.PaymentCreator;
import org.example.sharedprompts.domain.payment.application.command.service.request.PaymentPreparationHandler;
import org.example.sharedprompts.domain.payment.application.dto.AmountProcessingResult;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PointType;
import org.example.sharedprompts.domain.payment.infrastructure.monitoring.PaymentLoggingService;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.PaymentJpaAdapter;
import org.example.sharedprompts.domain.payment.service.point.PointService;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.dto.payment.request.PaymentRequestDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRequestService {

    private final PaymentValidationService validationService;
    private final PaymentAmountProcessingService amountProcessingService;
    private final PaymentCreator paymentCreator;
    private final PaymentPreparationHandler preparationHandler;
    private final PaymentJpaAdapter paymentJpaAdapter;
    private final PaymentLoggingService loggingService;
    private final PointService pointService;

    @Transactional
    public Payment processRequest(Long userId, PaymentRequestDto request, User user) {
        log.info("결제 요청 시작: userId={}, paymentMethod={}, amount={}",
                userId, request.getPaymentMethod(), request.getAmount());

        validationService.validateDailyLimit(userId, user.getTier());

        AmountProcessingResult amountResult = amountProcessingService.processPaymentAmount(
                userId,
                request.getAmount(),
                request.getCurrency(),
                request.getUsePointAmount()
        );

        Payment payment = null;
        try {
            payment = paymentCreator.createPayment(user, request, amountResult);

            // Payment를 먼저 저장하여 ID를 생성
            payment = paymentJpaAdapter.save(payment);

            // 결제 준비를 처리하고 메타데이터를 업데이트
            // (idempotencyKey, externalPaymentId, metadata 등이 업데이트될 수 있음)
            payment = preparationHandler.processPreparationIfNeeded(payment, amountResult, userId, request);

            // 결제 준비 과정에서 업데이트된 모든 변경사항 저장
            // (JPA는 변경사항이 없으면 최적화하므로 성능 문제 없음)
            payment = paymentJpaAdapter.save(payment);

            loggingService.logPaymentRequest(payment);

            log.info("결제 요청 완료: paymentId={}, userId={}, paymentMethod={}",
                    payment.getId(), userId, request.getPaymentMethod());

            return payment;
        } catch (Exception e) {
            BigDecimal usedPointAmount = amountResult.usedPointAmount();
            if (usedPointAmount != null && usedPointAmount.compareTo(BigDecimal.ZERO) > 0) {
                recoverPointsInNewTransaction(userId, payment != null ? payment.getId() : null, usedPointAmount, e);
            }
            throw e;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void recoverPointsInNewTransaction(Long userId, Long paymentId, BigDecimal usedPointAmount, Exception originalException) {
        try {
            pointService.addPointsDirectly(
                    userId,
                    paymentId,
                    usedPointAmount,
                    PointType.PAYMENT_FAILED,
                    "결제 요청 실패로 인한 포인트 복구"
            );
            log.info("결제 요청 실패로 인한 포인트 복구 완료: userId={}, refundPointAmount={}, error={}",
                    userId, usedPointAmount, originalException.getMessage());
        } catch (Exception pointException) {
            log.error("결제 요청 실패 후 포인트 복구 실패: userId={}, amount={}, error={}",
                    userId, usedPointAmount, pointException.getMessage(), pointException);
        }
    }
}


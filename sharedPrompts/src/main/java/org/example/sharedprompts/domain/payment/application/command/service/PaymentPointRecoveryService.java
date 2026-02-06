package org.example.sharedprompts.domain.payment.application.command.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.domain.enums.PointType;
import org.example.sharedprompts.domain.payment.service.point.PointService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentPointRecoveryService {

    private final PointService pointService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recoverPointsForPaymentFailure(Long userId, Long paymentId, BigDecimal usedPointAmount, Exception originalException) {
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


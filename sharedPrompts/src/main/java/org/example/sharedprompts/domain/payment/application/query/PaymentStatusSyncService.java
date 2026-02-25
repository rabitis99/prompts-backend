package org.example.sharedprompts.domain.payment.application.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.dto.payment.response.PaymentStatusResponseDto;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentStatusSyncService {

    private static final String LOG_PREFIX = "[PaymentStatusSync] ";

    private final PaymentStatusSyncExecutor syncExecutor;

    @Transactional
    public PaymentStatusResponseDto syncPaymentStatusForAdmin(Long paymentId) {
        log.info(LOG_PREFIX + "관리자 결제 상태 동기화 요청: paymentId={}", paymentId);

        try {
            return syncExecutor.doSyncPaymentStatusById(paymentId);
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn(LOG_PREFIX + "낙관적 락 충돌 발생, 최신 상태로 재조회: paymentId={}", paymentId, e);
            return syncExecutor.doSyncPaymentStatusById(paymentId);
        }
    }
}


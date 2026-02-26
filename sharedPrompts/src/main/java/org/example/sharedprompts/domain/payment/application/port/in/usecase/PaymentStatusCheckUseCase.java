package org.example.sharedprompts.domain.payment.application.port.in.usecase;

import org.example.sharedprompts.domain.payment.application.port.in.command.PaymentStatusCheckQuery;
import org.example.sharedprompts.domain.payment.application.port.in.result.PaymentStatusResult;

/**
 * 결제 상태 조회 유스케이스 포트
 * 특정 결제의 상태를 조회하는 비즈니스 로직
 */
public interface PaymentStatusCheckUseCase {

    /**
     * 결제 상태를 조회한다.
     *
     * @param query 결제 상태 조회 쿼리
     * @return 결제 상태 결과
     */
    PaymentStatusResult checkStatus(PaymentStatusCheckQuery query);
}

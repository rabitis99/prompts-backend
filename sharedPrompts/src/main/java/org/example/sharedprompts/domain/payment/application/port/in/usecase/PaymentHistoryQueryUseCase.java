package org.example.sharedprompts.domain.payment.application.port.in.usecase;

import org.example.sharedprompts.domain.payment.application.port.in.command.PaymentHistoryQuery;
import org.example.sharedprompts.domain.payment.application.port.in.result.PaymentHistoryResult;
import org.springframework.data.domain.Page;

/**
 * 결제 내역 조회 유스케이스 포트
 * 사용자의 결제 내역을 조회하는 비즈니스 로직
 */
public interface PaymentHistoryQueryUseCase {

    /**
     * 결제 내역을 조회한다.
     *
     * @param query 결제 내역 조회 쿼리
     * @return 페이징된 결제 내역
     */
    Page<PaymentHistoryResult> getHistory(PaymentHistoryQuery query);
}

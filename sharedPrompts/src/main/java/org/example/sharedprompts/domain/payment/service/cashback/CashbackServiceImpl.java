package org.example.sharedprompts.domain.payment.service.cashback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.service.cashback.facade.CashbackFacade;
import org.example.sharedprompts.dto.payment.response.CashbackResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 캐시백 서비스 구현체
 * 
 * <p>도메인 메서드 호출 및 서비스 조율만 담당
 * - 실제 로직은 CashbackFacade에 위임
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CashbackServiceImpl implements CashbackService {

    private final CashbackFacade cashbackFacade;

    @Override
    public Page<CashbackResponseDto> getCashbackHistory(Long customerId, Pageable pageable) {
        return cashbackFacade.getCashbackHistory(customerId, pageable);
    }

    @Override
    @Transactional
    public void accumulateCashback(Long userId, Long paymentId, BigDecimal paymentAmount) {
        cashbackFacade.accumulateCashback(userId, paymentId, paymentAmount);
    }

    @Override
    @Transactional
    public void payCashback(Long userId, Long cashbackId) {
        cashbackFacade.payCashback(userId, cashbackId);
    }

    @Override
    public Page<CashbackResponseDto> getUnpaidCashbacks(Long customerId, Pageable pageable) {
        return cashbackFacade.getUnpaidCashbacks(customerId, pageable);
    }

    @Override
    public BigDecimal getUnpaidCashbackTotal(Long userId) {
        return cashbackFacade.getUnpaidCashbackTotal(userId);
    }

    // ============ 관리자용 메서드 ============

    @Override
    public BigDecimal getAllUnpaidCashbackTotal() {
        return cashbackFacade.getAllUnpaidCashbackTotal();
    }

    @Override
    public Page<CashbackResponseDto> getAllUnpaidCashbacks(Pageable pageable) {
        return cashbackFacade.getAllUnpaidCashbacks(pageable);
    }

    @Override
    @Transactional
    public void payCashbackForAdmin(Long cashbackId) {
        cashbackFacade.payCashbackForAdmin(cashbackId);
    }
}


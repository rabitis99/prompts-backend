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

    /**
     * 캐시백 적립
     * 
     * <p>트랜잭션은 CashbackFacade 내부의 TransactionTemplate으로 관리됩니다.
     * 락 획득 → 트랜잭션 시작 순서를 보장하기 위해 @Transactional을 제거했습니다.
     */
    @Override
    public void accumulateCashback(Long userId, Long paymentId, BigDecimal paymentAmount) {
        cashbackFacade.accumulateCashback(userId, paymentId, paymentAmount);
    }

    /**
     * 캐시백 지급
     * 
     * <p>트랜잭션은 CashbackFacade 내부의 TransactionTemplate으로 관리됩니다.
     * 락 획득 → 트랜잭션 시작 순서를 보장하기 위해 @Transactional을 제거했습니다.
     */
    @Override
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

    /**
     * 캐시백 지급 (관리자용)
     * 
     * <p>트랜잭션은 CashbackFacade 내부의 TransactionTemplate으로 관리됩니다.
     * 락 획득 → 트랜잭션 시작 순서를 보장하기 위해 @Transactional을 제거했습니다.
     */
    @Override
    public void payCashbackForAdmin(Long cashbackId) {
        cashbackFacade.payCashbackForAdmin(cashbackId);
    }
}


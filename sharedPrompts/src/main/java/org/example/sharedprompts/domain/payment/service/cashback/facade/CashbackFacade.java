package org.example.sharedprompts.domain.payment.service.cashback.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Cashback;
import org.example.sharedprompts.domain.payment.config.RewardProperties;
import org.example.sharedprompts.domain.payment.repository.cashback.CashbackRepository;
import org.example.sharedprompts.domain.payment.service.cashback.amount.CashbackAmountService;
import org.example.sharedprompts.domain.payment.service.cashback.execution.CashbackExecutionService;
import org.example.sharedprompts.domain.payment.service.cashback.lock.CashbackLockService;
import org.example.sharedprompts.domain.payment.service.cashback.validation.CashbackValidationService;
import org.example.sharedprompts.dto.payment.response.CashbackResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 캐시백 Facade
 * 
 * <p>클라이언트 단일 진입점, 내부적으로 세분화된 서비스들을 조율
 * - CashbackValidationService: 검증
 * - CashbackAmountService: 금액 계산
 * - CashbackExecutionService: 적립/지급 실행
 * - CashbackLockService: 락 관리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CashbackFacade {

    private final CashbackRepository cashbackRepository;
    private final RewardProperties rewardProperties;
    private final CashbackValidationService validationService;
    private final CashbackAmountService amountService;
    private final CashbackExecutionService executionService;
    private final CashbackLockService lockService;

    /**
     * 캐시백 이력 조회
     */
    @Transactional(readOnly = true)
    public Page<CashbackResponseDto> getCashbackHistory(Long customerId, Pageable pageable) {
        return cashbackRepository.findByUserIdWithFetchJoin(customerId, pageable)
                .map(CashbackResponseDto::from);
    }

    /**
     * 캐시백 적립
     *
     * <p>동시성 문제 방지를 위해 paymentId 기반 분산 락을 적용합니다.
     * TOCTOU 문제를 방지하기 위해 락 내에서 중복 검증을 수행합니다.
     */
    public void accumulateCashback(Long userId, Long paymentId, BigDecimal paymentAmount) {
        // 동시성 문제 방지를 위해 paymentId 기반 분산 락 적용
        lockService.executeWithLockForPayment(paymentId, () -> {
            // 락 내에서 중복 적립 방지 검증 (TOCTOU 방지)
            if (validationService.isAlreadyAccumulated(paymentId)) {
                return null;
            }

            // 캐시백 금액 계산
            BigDecimal cashbackRate = rewardProperties.getCashbackRate();
            BigDecimal cashbackAmount = amountService.calculateCashbackAmount(paymentAmount, cashbackRate);

            // 캐시백 적립 실행
            if (cashbackAmount.compareTo(BigDecimal.ZERO) > 0) {
                executionService.accumulateCashback(userId, paymentId, cashbackAmount, paymentAmount, cashbackRate);
            }
            return null;
        });
    }

    /**
     * 캐시백 지급
     */
    @Transactional
    public void payCashback(Long userId, Long cashbackId) {
        // 동시성 문제 방지를 위해 분산 락 적용
        lockService.executeWithLock(cashbackId, () -> {
            Cashback cashback = cashbackRepository.findById(cashbackId)
                    .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "캐시백을 찾을 수 없습니다."));

            // 검증
            validationService.validateOwnership(cashback, userId);
            validationService.validateNotPaid(cashback);

            // 지급 실행 (이미 조회한 엔티티 전달하여 중복 조회 제거)
            executionService.payCashback(cashback, userId, false);
            return null;
        });
    }

    /**
     * 미지급 캐시백 조회
     */
    @Transactional(readOnly = true)
    public Page<CashbackResponseDto> getUnpaidCashbacks(Long customerId, Pageable pageable) {
        return cashbackRepository.findUnpaidByUserIdWithFetchJoin(customerId, pageable)
                .map(CashbackResponseDto::from);
    }

    /**
     * 미지급 캐시백 총액 조회
     */
    @Transactional(readOnly = true)
    public BigDecimal getUnpaidCashbackTotal(Long userId) {
        BigDecimal total = cashbackRepository.getUnpaidCashbackTotal(userId);
        return total != null ? total : BigDecimal.ZERO;
    }

    // ============ 관리자용 메서드 ============

    /**
     * 전체 미지급 캐시백 총액 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public BigDecimal getAllUnpaidCashbackTotal() {
        BigDecimal total = cashbackRepository.getAllUnpaidCashbackTotal();
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * 전체 미지급 캐시백 목록 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public Page<CashbackResponseDto> getAllUnpaidCashbacks(Pageable pageable) {
        return cashbackRepository.findAllUnpaidWithFetchJoin(pageable)
                .map(CashbackResponseDto::from);
    }

    /**
     * 캐시백 지급 (관리자용 - 소유권 검증 없음)
     */
    @Transactional
    public void payCashbackForAdmin(Long cashbackId) {
        // 동시성 문제 방지를 위해 분산 락 적용
        lockService.executeWithLock(cashbackId, () -> {
            Cashback cashback = cashbackRepository.findById(cashbackId)
                    .orElseThrow(() -> new org.example.sharedprompts.global.exception.ApiException(
                            org.example.sharedprompts.global.exception.ErrorCode.NOT_FOUND,
                            "캐시백을 찾을 수 없습니다."
                    ));

            // 관리자는 소유권 검증 없이 미지급 상태만 검증
            validationService.validateNotPaid(cashback);

            // 지급 실행 (이미 조회한 엔티티 전달하여 중복 조회 제거)
            Long userId = cashback.getUser().getId();
            executionService.payCashback(cashback, userId, true);
            return null;
        });
    }
}


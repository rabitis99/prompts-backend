package org.example.sharedprompts.domain.payment.service.cashback.execution;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Cashback;
import org.example.sharedprompts.domain.payment.repository.cashback.CashbackRepository;
import org.example.sharedprompts.domain.payment.service.point.PointService;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 캐시백 실행 서비스
 * 
 * <p>단일 책임: 실제 캐시백 적립/지급 수행만 담당
 * - 캐시백 적립
 * - 캐시백 지급 (포인트 전환)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CashbackExecutionService {

    private final CashbackRepository cashbackRepository;
    private final UserRepository userRepository;
    private final PointService pointService;

    /**
     * 캐시백 적립
     */
    @Transactional
    public Cashback accumulateCashback(Long userId, Long paymentId, BigDecimal cashbackAmount, 
                                       BigDecimal paymentAmount, BigDecimal rate) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        
        Cashback cashback = Cashback.builder()
                .user(user)
                .paymentId(paymentId)
                .amount(cashbackAmount)
                .rate(rate)
                .paymentAmount(paymentAmount)
                .description("결제 캐시백 적립")
                .paid(false)
                .build();

        cashback = cashbackRepository.save(cashback);
        log.info("캐시백 적립 완료: paymentId={}, userId={}, amount={}", paymentId, userId, cashbackAmount);
        
        return cashback;
    }

    /**
     * 캐시백 지급 (포인트 전환)
     */
    @Transactional
    public void payCashback(Long userId, Long cashbackId) {
        Cashback cashback = cashbackRepository.findById(cashbackId)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.NOT_FOUND,
                        "캐시백을 찾을 수 없습니다."
                ));

        // 캐시백을 포인트로 전환하여 지급
        BigDecimal cashbackAmount = cashback.getAmount();
        Long paymentId = cashback.getPaymentId();
        
        try {
            // 캐시백 금액을 포인트로 적립 (1원 = 1포인트로 전환)
            // paymentId는 캐시백이 발생한 원본 결제 ID를 사용
            pointService.addPointsDirectly(userId, paymentId, cashbackAmount, 
                    "캐시백 지급: " + cashbackAmount + "원");
            
            // 지급 완료 처리 (도메인 메서드 사용)
            cashback.markAsPaid();
            cashbackRepository.save(cashback);
            
            log.info("캐시백 지급 완료: cashbackId={}, userId={}, amount={}, paymentId={}", 
                    cashbackId, userId, cashbackAmount, paymentId);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("캐시백 지급 실패: cashbackId={}, userId={}, amount={}, error={}",
                    cashbackId, userId, cashbackAmount, e.getMessage(), e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "캐시백 지급 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 관리자용 캐시백 지급 (포인트 전환)
     */
    @Transactional
    public void payCashbackForAdmin(Long cashbackId) {
        Cashback cashback = cashbackRepository.findById(cashbackId)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.NOT_FOUND,
                        "캐시백을 찾을 수 없습니다."
                ));

        // 관리자는 소유권 검증 없이 지급 가능
        // 캐시백을 포인트로 전환하여 지급
        Long userId = cashback.getUser().getId();
        BigDecimal cashbackAmount = cashback.getAmount();
        Long paymentId = cashback.getPaymentId();
        
        try {
            // 캐시백 금액을 포인트로 적립 (1원 = 1포인트로 전환)
            pointService.addPointsDirectly(userId, paymentId, cashbackAmount, 
                    "캐시백 지급 (관리자): " + cashbackAmount + "원");
            
            // 지급 완료 처리 (도메인 메서드 사용)
            cashback.markAsPaid();
            cashbackRepository.save(cashback);
            
            log.info("캐시백 지급 완료 (관리자): cashbackId={}, userId={}, amount={}, paymentId={}", 
                    cashbackId, userId, cashbackAmount, paymentId);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("캐시백 지급 실패 (관리자): cashbackId={}, userId={}, amount={}, error={}",
                    cashbackId, userId, cashbackAmount, e.getMessage(), e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "캐시백 지급 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}


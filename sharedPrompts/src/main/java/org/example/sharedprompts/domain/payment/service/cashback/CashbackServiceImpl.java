package org.example.sharedprompts.domain.payment.service.cashback;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.Cashback;
import org.example.sharedprompts.domain.payment.config.PaymentProperties;
import org.example.sharedprompts.domain.payment.repository.CashbackRepository;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.dto.payment.response.CashbackResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 캐시백 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CashbackServiceImpl implements CashbackService {

    private final CashbackRepository cashbackRepository;
    private final PaymentProperties paymentProperties;
    private final UserRepository userRepository;

    @Override
    public Page<CashbackResponseDto> getCashbackHistory(Long customerId, Pageable pageable) {
        return cashbackRepository.findByUserIdWithFetchJoin(customerId, pageable)
                .map(CashbackResponseDto::from);
    }

    @Override
    @Transactional
    public void accumulateCashback(Long userId, Long paymentId, BigDecimal paymentAmount) {
        // 캐시백 적립률 적용
        BigDecimal cashbackAmount = paymentAmount.multiply(BigDecimal.valueOf(paymentProperties.getCashbackRate()))
                .setScale(2, RoundingMode.DOWN); // 소수점 둘째 자리까지

        if (cashbackAmount.compareTo(BigDecimal.ZERO) > 0) {
            // 사용자 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
            
            Cashback cashback = Cashback.builder()
                    .user(user)
                    .paymentId(paymentId)
                    .amount(cashbackAmount)
                    .rate(BigDecimal.valueOf(paymentProperties.getCashbackRate()))
                    .paymentAmount(paymentAmount)
                    .description("결제 캐시백 적립")
                    .paid(false)
                    .build();

            cashbackRepository.save(cashback);
        }
    }

    @Override
    @Transactional
    public void payCashback(Long userId, Long cashbackId) {
        Cashback cashback = cashbackRepository.findById(cashbackId)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.NOT_FOUND,
                        "캐시백을 찾을 수 없습니다."
                ));

        if (!cashback.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "캐시백 지급 권한이 없습니다.");
        }

        if (cashback.isPaid()) {
            throw new ApiException(
                    ErrorCode.BAD_REQUEST,
                    "이미 지급된 캐시백입니다."
            );
        }

        // 실제 지급 로직은 여기에 구현 (예: 계좌 이체, 포인트 전환 등)
        cashback.markAsPaid();
        cashbackRepository.save(cashback);
    }

    @Override
    public Page<CashbackResponseDto> getUnpaidCashbacks(Long customerId, Pageable pageable) {
        return cashbackRepository.findUnpaidByUserIdWithFetchJoin(customerId, pageable)
                .map(CashbackResponseDto::from);
    }

    @Override
    public BigDecimal getUnpaidCashbackTotal(Long userId) {
        return cashbackRepository.getUnpaidCashbackTotal(userId)
                .orElse(BigDecimal.ZERO);
    }
}


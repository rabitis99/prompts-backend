package org.example.sharedprompts.domain.payment.service.user.tier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.domain.entity.UserTierHistory;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.domain.enums.UserTier;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.PaymentJpaAdapter;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.UserTierHistoryJpaAdapter;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.dto.payment.request.TierChangeRequestDto;
import org.example.sharedprompts.dto.payment.response.TierInfoResponseDto;
import org.example.sharedprompts.dto.payment.response.UserTierHistoryResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 사용자 티어 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserTierServiceImpl implements UserTierService {

    private final UserRepository userRepository;
    private final PaymentJpaAdapter paymentJpaAdapter;
    private final UserTierHistoryJpaAdapter tierHistoryJpaAdapter;
    private final TierUpgradePolicy tierUpgradePolicy;

    @Override
    public UserTier getTier(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        return user.getTier();
    }

    @Override
    public TierInfoResponseDto getTierInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        UserTier tier = user.getTier();
        int dailyLimit = tier.getDailyLimit();
        long todayUsedCount = paymentJpaAdapter.countTodaySuccessfulPayments(userId, PaymentStatus.SUCCESS);
        int remainingCount = (int) Math.max(0, dailyLimit - todayUsedCount);

        return TierInfoResponseDto.from(user, dailyLimit, (int) todayUsedCount, remainingCount);
    }

    @Override
    @Transactional
    public void changeTier(Long userId, TierChangeRequestDto request, Long changedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        UserTier currentTier = user.getTier();
        UserTier newTier = request.getTier();

        // 동일한 티어로 변경 불가
        if (currentTier == newTier) {
            throw new ApiException(ErrorCode.SAME_TIER);
        }

        // 티어 변경 이력 저장
        UserTierHistory history = UserTierHistory.builder()
                .user(user)
                .previousTier(currentTier)
                .newTier(newTier)
                .changedBy(changedBy)
                .reason(request.getReasonOrDefault())
                .build();
        tierHistoryJpaAdapter.save(history);

        user.changeTier(newTier);
        recalculateDailyLimit(user);
    }

    private void recalculateDailyLimit(User user) {
        UserTier currentTier = user.getTier();
        int dailyLimit = currentTier.getDailyLimit();
        long todayUsedCount = paymentJpaAdapter.countTodaySuccessfulPayments(user.getId(), PaymentStatus.SUCCESS);
        int remainingCount = (int) Math.max(0, dailyLimit - todayUsedCount);
        
        log.info("일일 제한 재계산: userId={}, tier={}, dailyLimit={}, todayUsedCount={}, remainingCount={}", 
                user.getId(), currentTier.name(), dailyLimit, todayUsedCount, remainingCount);
        
        if (todayUsedCount >= dailyLimit) {
            log.warn("티어 변경 후 일일 제한 초과 상태: userId={}, tier={}, todayUsedCount={}, dailyLimit={}", 
                    user.getId(), currentTier.name(), todayUsedCount, dailyLimit);
        } else if (todayUsedCount >= dailyLimit * 0.8) {
            log.warn("티어 변경 후 일일 제한 근접: userId={}, tier={}, todayUsedCount={}, dailyLimit={}, usageRate={}%", 
                    user.getId(), currentTier.name(), todayUsedCount, dailyLimit, 
                    (int) (todayUsedCount * 100.0 / dailyLimit));
        }
    }

    @Override
    @Transactional
    public void recalculateDailyLimit(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        recalculateDailyLimit(user);
    }

    @Override
    public Page<UserTierHistoryResponseDto> getTierHistory(Long userId, Pageable pageable) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        
        return tierHistoryJpaAdapter.findByUserIdWithFetchJoin(userId, pageable)
                .map(UserTierHistoryResponseDto::from);
    }

    @Override
    @Transactional
    public void upgradeTierIfEligible(Long userId, BigDecimal paymentAmount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        BigDecimal totalPaymentAmount = Optional.ofNullable(
                paymentJpaAdapter.sumTotalPaymentAmount(userId, PaymentStatus.SUCCESS))
                .orElse(BigDecimal.ZERO);
        
        UserTier currentTier = user.getTier();

        UserTier calculatedTier = tierUpgradePolicy.calculateTier(totalPaymentAmount, currentTier);
        if (calculatedTier.compareTo(currentTier) > 0) {
            log.info("티어 자동 업그레이드: userId={}, currentTier={}, newTier={}, totalPaymentAmount={}",
                    userId, currentTier, calculatedTier, totalPaymentAmount);

            UserTierHistory history = UserTierHistory.builder()
                    .user(user)
                    .previousTier(currentTier)
                    .newTier(calculatedTier)
                    .changedBy(userId)
                    .reason("결제 성공으로 인한 자동 업그레이드")
                    .build();
            tierHistoryJpaAdapter.save(history);

            user.changeTier(calculatedTier);
            recalculateDailyLimit(user);
        }
    }
}


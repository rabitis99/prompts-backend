package org.example.sharedprompts.domain.payment.service.user.tier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.domain.entity.ModuleUsage;
import org.example.sharedprompts.domain.payment.domain.entity.UserTierHistory;
import org.example.sharedprompts.domain.payment.domain.enums.ModuleType;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.domain.enums.UserTier;
import org.example.sharedprompts.domain.payment.domain.policy.TierLimitPolicy;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.PaymentJpaAdapter;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.UserTierHistoryJpaAdapter;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.repository.moduleusage.ModuleUsageRepository;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.dto.payment.request.TierChangeRequestDto;
import org.example.sharedprompts.dto.payment.response.ConsumeModuleUsageResponseDto;
import org.example.sharedprompts.dto.payment.response.TierInfoResponseDto;
import org.example.sharedprompts.dto.payment.response.UserTierHistoryResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 사용자 티어 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserTierServiceImpl implements UserTierService {

    private static final ModuleType[] USAGE_MODULE_TYPES = Arrays.stream(ModuleType.values())
            .filter(t -> t != ModuleType.UNKNOWN)
            .toArray(ModuleType[]::new);

    private final UserRepository userRepository;
    private final PaymentJpaAdapter paymentJpaAdapter;
    private final UserTierHistoryJpaAdapter tierHistoryJpaAdapter;
    private final ModuleUsageRepository moduleUsageRepository;
    private final TierUpgradePolicy tierUpgradePolicy;
    private final TierLimitPolicy tierLimitPolicy;

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
        int limit = tierLimitPolicy.getDailyLimit(tier);
        int todayUsed = computeTodayUsedWeighted(userId);
        int remaining = Math.max(0, limit - todayUsed);

        Map<String, Integer> remainingByModuleType = new LinkedHashMap<>();
        for (ModuleType mt : USAGE_MODULE_TYPES) {
            int consumption = tierLimitPolicy.getConsumptionAmount(mt);
            remainingByModuleType.put(mt.name(), Math.max(0, remaining - consumption));
        }

        return TierInfoResponseDto.from(user, limit, todayUsed, remaining, remainingByModuleType);
    }

    @Override
    public TierInfoResponseDto getTierInfo(Long userId, ModuleType moduleType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        UserTier tier = user.getTier();
        int limit = tierLimitPolicy.getDailyLimit(tier);
        int todayUsed = computeTodayUsedWeighted(userId);
        int remaining = Math.max(0, limit - todayUsed);
        return TierInfoResponseDto.fromModule(user, moduleType, limit, todayUsed, remaining);
    }

    @Override
    public long getTodayUsedCount(Long userId) {
        return computeTodayUsedWeighted(userId);
    }

    private int computeTodayUsedWeighted(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        long todayPaymentCount = paymentJpaAdapter.countTodaySuccessfulPayments(userId, PaymentStatus.SUCCESS);
        int weightedModule = 0;
        for (ModuleType mt : USAGE_MODULE_TYPES) {
            long count = moduleUsageRepository.countTodayByUserIdAndModuleType(userId, mt, startOfDay, endOfDay);
            weightedModule += (int) (count * tierLimitPolicy.getConsumptionAmount(mt));
        }
        return (int) todayPaymentCount + weightedModule;
    }

    @Override
    public long getTodayPaymentCount(Long userId) {
        return paymentJpaAdapter.countTodaySuccessfulPayments(userId, PaymentStatus.SUCCESS);
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
        int dailyLimit = tierLimitPolicy.getDailyLimit(currentTier);
        int todayUsed = computeTodayUsedWeighted(user.getId());
        int remaining = Math.max(0, dailyLimit - todayUsed);

        log.info("일일 제한 재계산: userId={}, tier={}, dailyLimit={}, todayUsedWeighted={}, remaining={}",
                user.getId(), currentTier.name(), dailyLimit, todayUsed, remaining);

        long todayPaymentCount = paymentJpaAdapter.countTodaySuccessfulPayments(user.getId(), PaymentStatus.SUCCESS);
        if (todayPaymentCount >= dailyLimit) {
            log.warn("티어 변경 후 일일 결제 제한 초과: userId={}, tier={}, todayPaymentCount={}, dailyLimit={}",
                    user.getId(), currentTier.name(), todayPaymentCount, dailyLimit);
        } else if (todayPaymentCount >= dailyLimit * 0.8) {
            log.warn("티어 변경 후 일일 결제 제한 근접: userId={}, tier={}, todayPaymentCount={}, dailyLimit={}, usageRate={}%",
                    user.getId(), currentTier.name(), todayPaymentCount, dailyLimit,
                    (int) (todayPaymentCount * 100.0 / dailyLimit));
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

    @Override
    @Transactional
    public ConsumeModuleUsageResponseDto consumeModuleUsage(Long userId, ModuleType moduleType) {
        // 비관적 락으로 동일 사용자에 대한 동시 차감을 직렬화 (검증·차감을 하나의 임계구역으로)
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        UserTier tier = user.getTier();
        int limit = tierLimitPolicy.getDailyLimit(tier);
        int consumption = tierLimitPolicy.getConsumptionAmount(moduleType);
        int todayUsed = computeTodayUsedWeighted(userId);
        int remaining = Math.max(0, limit - todayUsed);

        if (remaining < consumption) {
            throw new ApiException(ErrorCode.MODULE_DAILY_LIMIT_EXCEEDED);
        }

        ModuleUsage usage = ModuleUsage.builder()
                .user(user)
                .moduleType(moduleType)
                .build();
        moduleUsageRepository.save(usage);
        int usedAfter = todayUsed + consumption;
        int remainingAfter = Math.max(0, limit - usedAfter);
        log.debug("모듈 사용 차감: userId={}, moduleType={}, consumption={}, usedWeighted={}, remaining={}", userId, moduleType, consumption, usedAfter, remainingAfter);
        return ConsumeModuleUsageResponseDto.builder()
                .moduleType(moduleType)
                .limit(limit)
                .consumptionAmount(consumption)
                .usedToday(usedAfter)
                .remaining(remainingAfter)
                .build();
    }
}


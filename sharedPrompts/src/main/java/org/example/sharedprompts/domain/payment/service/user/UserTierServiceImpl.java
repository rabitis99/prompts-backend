package org.example.sharedprompts.domain.payment.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.UserTierHistory;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.enums.UserTier;
import org.example.sharedprompts.domain.payment.repository.payment.PaymentRepository;
import org.example.sharedprompts.domain.payment.repository.UserTierHistoryRepository;
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

/**
 * 사용자 티어 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserTierServiceImpl implements UserTierService {

    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final UserTierHistoryRepository tierHistoryRepository;

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
        long todayUsedCount = paymentRepository.countTodaySuccessfulPayments(userId, PaymentStatus.SUCCESS);
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
        tierHistoryRepository.save(history);

        // 사용자 티어 변경
        // JPA dirty checking으로 트랜잭션 커밋 시 자동 반영되므로 명시적 save 불필요
        user.changeTier(newTier);
        
        // 티어 변경 후 일일 제한 재계산
        recalculateDailyLimit(userId);
    }

    @Override
    @Transactional
    public void recalculateDailyLimit(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        
        UserTier currentTier = user.getTier();
        int dailyLimit = currentTier.getDailyLimit();
        long todayUsedCount = paymentRepository.countTodaySuccessfulPayments(userId, PaymentStatus.SUCCESS);
        int remainingCount = (int) Math.max(0, dailyLimit - todayUsedCount);
        
        // 티어 변경 후 일일 제한 재계산 및 로깅
        log.info("일일 제한 재계산: userId={}, tier={}, dailyLimit={}, todayUsedCount={}, remainingCount={}", 
                userId, currentTier.name(), dailyLimit, todayUsedCount, remainingCount);
        
        // 새로운 티어의 제한을 이미 초과한 경우 경고
        if (todayUsedCount >= dailyLimit) {
            log.warn("티어 변경 후 일일 제한 초과 상태: userId={}, tier={}, todayUsedCount={}, dailyLimit={}", 
                    userId, currentTier.name(), todayUsedCount, dailyLimit);
        } else if (todayUsedCount >= dailyLimit * 0.8) {
            // 제한의 80% 이상 사용 시 경고
            log.warn("티어 변경 후 일일 제한 근접: userId={}, tier={}, todayUsedCount={}, dailyLimit={}, usageRate={}%", 
                    userId, currentTier.name(), todayUsedCount, dailyLimit, 
                    (int) (todayUsedCount * 100.0 / dailyLimit));
        }
    }

    @Override
    public Page<UserTierHistoryResponseDto> getTierHistory(Long userId, Pageable pageable) {
        return tierHistoryRepository.findByUserIdWithFetchJoin(userId, pageable)
                .map(UserTierHistoryResponseDto::from);
    }
}


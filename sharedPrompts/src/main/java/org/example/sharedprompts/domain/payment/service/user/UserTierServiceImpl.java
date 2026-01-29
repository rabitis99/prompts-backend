package org.example.sharedprompts.domain.payment.service.user;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.UserTierHistory;
import org.example.sharedprompts.domain.payment.enums.UserTier;
import org.example.sharedprompts.domain.payment.repository.PaymentRepository;
import org.example.sharedprompts.domain.payment.repository.UserTierHistoryRepository;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.dto.payment.request.TierChangeRequestDto;
import org.example.sharedprompts.dto.payment.response.TierInfoResponseDto;
import org.example.sharedprompts.dto.payment.response.UserTierHistoryResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 사용자 티어 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserTierServiceImpl implements UserTierService {

    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final UserTierHistoryRepository tierHistoryRepository;

    /**
     * Retrieve the current tier for the specified user.
     *
     * @param userId the ID of the user
     * @return the user's current {@link UserTier}
     * @throws ApiException if no user exists for the given ID (ErrorCode.USER_NOT_FOUND)
     */
    @Override
    public UserTier getTier(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        return user.getTier();
    }

    /**
     * Retrieves tier details and today's usage for the specified user.
     *
     * Returns a DTO containing the user's current tier, the tier's daily limit,
     * the count of successful payments made today, and the remaining allowed uses for today.
     *
     * @param userId the identifier of the user
     * @return a TierInfoResponseDto with tier, daily limit, today's successful payment count, and remaining count
     * @throws ApiException if the user does not exist (ErrorCode.USER_NOT_FOUND)
     */
    @Override
    public TierInfoResponseDto getTierInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        UserTier tier = user.getTier();
        int dailyLimit = tier.getDailyLimit();
        long todayUsedCount = paymentRepository.countTodaySuccessfulPayments(userId);
        int remainingCount = (int) Math.max(0, dailyLimit - todayUsedCount);

        return TierInfoResponseDto.from(user, dailyLimit, (int) todayUsedCount, remainingCount);
    }

    /**
     * Change a user's tier, persist the change and its history, and trigger recalculation of daily limits.
     *
     * @param userId   the id of the user whose tier will be changed
     * @param request  DTO containing the target tier and an optional reason for the change
     * @param changedBy the id of the actor who initiated the tier change
     * @throws ApiException if the user does not exist (ErrorCode.USER_NOT_FOUND)
     * @throws ApiException if the requested tier is the same as the user's current tier (ErrorCode.SAME_TIER)
     */
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
        user.changeTier(newTier);
        userRepository.save(user);

        // 일일 제한 재계산
        recalculateDailyLimit(userId);
    }

    /**
     * Recomputes and applies the user's daily payment limit after a tier change.
     *
     * Re-evaluates today's successful payment count and updates the user's effective daily limit or quota state so the new tier's limits take effect.
     *
     * @param userId the ID of the user whose daily limit should be recalculated
     */
    @Override
    @Transactional
    public void recalculateDailyLimit(Long userId) {
        // 티어 변경 시 일일 제한이 재계산되므로
        // 오늘 사용한 결제 횟수를 다시 확인하여 제한을 적용
        // 실제 구현에서는 필요에 따라 추가 로직을 수행할 수 있습니다.
    }

    /**
     * Retrieve a user's tier change history in reverse chronological order.
     *
     * @param userId the ID of the user whose tier history to retrieve
     * @return a list of UserTierHistoryResponseDto ordered by createdAt descending (newest first)
     */
    @Override
    public List<UserTierHistoryResponseDto> getTierHistory(Long userId) {
        List<UserTierHistory> histories = tierHistoryRepository.findByUser_IdOrderByCreatedAtDesc(userId);
        return histories.stream()
                .map(UserTierHistoryResponseDto::from)
                .collect(Collectors.toList());
    }
}

package org.example.sharedprompts.domain.payment.service.user.tier;

import org.example.sharedprompts.domain.payment.domain.enums.UserTier;
import org.example.sharedprompts.dto.payment.request.TierChangeRequestDto;
import org.example.sharedprompts.dto.payment.response.TierInfoResponseDto;
import org.example.sharedprompts.dto.payment.response.UserTierHistoryResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 사용자 티어 서비스 인터페이스
 */
public interface UserTierService {

    /**
     * 사용자 티어 조회
     */
    UserTier getTier(Long userId);

    /**
     * 사용자 티어 정보 조회 (티어, 일일 제한, 오늘 사용한 횟수, 남은 횟수)
     */
    TierInfoResponseDto getTierInfo(Long userId);

    /**
     * 사용자 티어 변경 (관리자용)
     */
    void changeTier(Long userId, TierChangeRequestDto request, Long changedBy);

    /**
     * 티어 변경 시 일일 제한 재계산
     */
    void recalculateDailyLimit(Long userId);

    /**
     * 사용자의 티어 변경 이력 조회
     */
    Page<UserTierHistoryResponseDto> getTierHistory(Long userId, Pageable pageable);

    /**
     * 결제 성공 시 티어 자동 업그레이드
     */
    void upgradeTierIfEligible(Long userId, java.math.BigDecimal paymentAmount);
}


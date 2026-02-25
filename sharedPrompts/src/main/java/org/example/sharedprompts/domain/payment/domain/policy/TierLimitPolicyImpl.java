package org.example.sharedprompts.domain.payment.domain.policy;

import org.example.sharedprompts.domain.payment.domain.enums.ModuleType;
import org.example.sharedprompts.domain.payment.domain.enums.UserTier;
import org.springframework.stereotype.Component;

/**
 * 티어별 일일 통합 한도 + 모듈별 1회 사용 시 차감량.
 * 오늘 사용량 = 결제 성공 횟수 + Σ(모듈별 오늘 사용 횟수 × getConsumptionAmount(모듈)).
 *
 * 차감량:
 *   TEXT    1  짧은 텍스트 생성
 *   EMAIL   1  이메일 1통 분량
 *   BLOG    2  블로그 긴 글, 토큰 사용 많음
 *   DOCUMENT 2  문서 생성
 *   IMAGE   3  이미지 생성 리소스/비용 큼
 *   LITERARY 3  장편·문학, 다단계 생성으로 가장 무거움
 */
@Component
public class TierLimitPolicyImpl implements TierLimitPolicy {

    @Override
    public int getDailyLimit(UserTier tier) {
        return tier.getDailyLimit();
    }

    @Override
    public int getConsumptionAmount(ModuleType moduleType) {
        if (moduleType == null || moduleType == ModuleType.UNKNOWN) {
            return 1;
        }
        return switch (moduleType) {
            case TEXT, EMAIL -> 1;
            case BLOG, DOCUMENT -> 2;
            case IMAGE, LITERARY -> 3;
            default -> 1;
        };
    }
}

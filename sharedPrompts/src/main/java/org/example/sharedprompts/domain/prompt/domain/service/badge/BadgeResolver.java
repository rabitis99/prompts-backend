package org.example.sharedprompts.domain.prompt.domain.service.badge;

import org.example.sharedprompts.domain.prompt.domain.model.result.QualityRubric;
import org.example.sharedprompts.domain.prompt.domain.model.result.VerifyResult;
import org.example.sharedprompts.domain.prompt.domain.value.quality.QualityBadge;
import org.example.sharedprompts.global.util.ValidationUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 내부 검증 지표를 UX 배지 목록으로 변환하는 도메인 서비스.
 *
 * <p>Spring 의존 없음 — {@code PromptDomainConfig}에서 생성·주입한다.
 *
 * <p>pass rate, repair count 등 수치는 배지로만 표현하며 외부에 노출하지 않는다.
 * 애플리케이션 서비스의 SRP 책임 분리 목적으로 추출되었다.
 */
public class BadgeResolver {

    /**
     * 검증 결과와 파이프라인 지표를 배지 목록으로 변환한다.
     *
     * @param lastResult       최종 Verify 결과
     * @param firstPassSuccess 최초 Verify 통과 여부
     * @param repairCount      Repair 시도 횟수
     * @param finallyPassed    최종 통과 여부
     * @return UX에 노출할 배지 목록
     */
    public List<QualityBadge> resolve(VerifyResult lastResult,
                                      boolean firstPassSuccess,
                                      int repairCount,
                                      boolean finallyPassed) {
        ValidationUtils.requireNonNull(lastResult, "lastResult");
        if (repairCount < 0) {
            throw new IllegalArgumentException("repairCount must be >= 0");
        }
        List<QualityBadge> badges = new ArrayList<>();

        if (finallyPassed) {
            badges.add(QualityBadge.CONDITIONS_MET);

            Boolean formatOk = lastResult.getItemResults().get(QualityRubric.RubricItem.FORMAT_COMPLIANCE);
            if (Boolean.TRUE.equals(formatOk)) {
                badges.add(QualityBadge.FORMAT_VERIFIED);
            }

            Boolean noProhibited = lastResult.getItemResults().get(QualityRubric.RubricItem.NO_PROHIBITED_CONTENT);
            if (Boolean.TRUE.equals(noProhibited)) {
                badges.add(QualityBadge.NO_PROHIBITED_CONTENT);
            }

            if (firstPassSuccess && repairCount == 0) {
                badges.add(QualityBadge.FAST_GENERATION);
            } else if (repairCount > 0) {
                badges.add(QualityBadge.REVERIFIED);
            }
        } else {
            // 최종 실패 시에도 금지어 없음 배지는 부여
            Boolean noProhibited = lastResult.getItemResults().get(QualityRubric.RubricItem.NO_PROHIBITED_CONTENT);
            if (Boolean.TRUE.equals(noProhibited)) {
                badges.add(QualityBadge.NO_PROHIBITED_CONTENT);
            }
        }

        return badges;
    }
}

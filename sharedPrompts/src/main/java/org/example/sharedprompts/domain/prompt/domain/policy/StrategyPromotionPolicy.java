package org.example.sharedprompts.domain.prompt.domain.policy;

import org.example.sharedprompts.domain.prompt.domain.value.PromptingStrategy;

/**
 * Experimental 전략의 Core/Objective-specific 승격 정책.
 *
 * <p>승격 점수 = 정확도 상승률 / (호출 증가 가중치 + 지연 증가 가중치)
 * <pre>
 * 호출 증가 가중치 = (실험군 평균 호출 수 - 대조군 평균 호출 수) × α
 * 지연 증가 가중치 = (실험군 평균 응답시간 - 대조군 평균 응답시간) / 기준 지연 × β
 * </pre>
 *
 * <p>플랜별 가중치:
 * <ul>
 *   <li>무료 플랜: α=0.6, β=0.4 (비용 우선)</li>
 *   <li>유료 플랜: α=0.3, β=0.3 (정확도 우선)</li>
 * </ul>
 */
public class StrategyPromotionPolicy {

    private static final double DEFAULT_THRESHOLD = 1.0;

    private final double alpha;  // 호출 증가 가중치
    private final double beta;   // 지연 증가 가중치
    private final double threshold;

    private StrategyPromotionPolicy(double alpha, double beta, double threshold) {
        this.alpha = alpha;
        this.beta = beta;
        this.threshold = threshold;
    }

    public static StrategyPromotionPolicy forFreeTier() {
        return new StrategyPromotionPolicy(0.6, 0.4, DEFAULT_THRESHOLD);
    }

    public static StrategyPromotionPolicy forPaidTier() {
        return new StrategyPromotionPolicy(0.3, 0.3, DEFAULT_THRESHOLD);
    }

    public static StrategyPromotionPolicy custom(double alpha, double beta, double threshold) {
        return new StrategyPromotionPolicy(alpha, beta, threshold);
    }

    /**
     * 전략 승격 여부 판정.
     *
     * @param accuracyGainPercent 정확도 상승률 (%)
     * @param callCountDiff       실험군 - 대조군 평균 호출 수 차이
     * @param latencyDiffMs       실험군 - 대조군 평균 응답시간 차이 (ms)
     * @param baseLatencyMs       기준 지연 (ms)
     */
    public boolean shouldPromote(
            double accuracyGainPercent,
            double callCountDiff,
            double latencyDiffMs,
            double baseLatencyMs
    ) {
        if (accuracyGainPercent <= 0) return false;
        if (baseLatencyMs <= 0) throw new IllegalArgumentException("기준 지연은 0보다 커야 합니다.");

        double callWeight = callCountDiff * alpha;
        double latencyWeight = (latencyDiffMs / baseLatencyMs) * beta;
        double score = accuracyGainPercent / (callWeight + latencyWeight);

        return score > threshold;
    }

    public double getAlpha() { return alpha; }
    public double getBeta() { return beta; }
    public double getThreshold() { return threshold; }

    /** 전략 승격 결과 */
    public record PromotionResult(PromptingStrategy strategy, boolean promoted, double score) {}
}

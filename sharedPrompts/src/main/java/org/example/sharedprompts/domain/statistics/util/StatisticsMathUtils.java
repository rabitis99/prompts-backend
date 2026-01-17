package org.example.sharedprompts.domain.statistics.util;

/**
 * 통계 관련 수학 유틸리티
 */
public class StatisticsMathUtils {

    private StatisticsMathUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 소수점 둘째 자리까지 반올림
     *
     * @param value 반올림할 값
     * @return 반올림된 값
     */
    public static double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /**
     * 백분율 계산 (0 ~ 100)
     *
     * @param numerator 분자
     * @param denominator 분모
     * @return 백분율 (소수점 둘째 자리까지)
     */
    public static double calculatePercentage(long numerator, long denominator) {
        if (denominator == 0) {
            return 0.0;
        }
        double percentage = (double) numerator / denominator * 100.0;
        return roundToTwoDecimals(percentage);
    }
}


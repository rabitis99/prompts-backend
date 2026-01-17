package org.example.sharedprompts.domain.statistics.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 통계 관련 날짜 유틸리티
 */
public class StatisticsDateUtils {

    private StatisticsDateUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 현재 시각 반환
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * 오늘 시작 시각 (00:00:00)
     */
    public static LocalDateTime todayStart() {
        return LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
    }

    /**
     * N일 전 시각
     */
    public static LocalDateTime daysAgo(int days) {
        return now().minusDays(days);
    }

    /**
     * 오늘 시작 시각부터 현재까지의 시간 범위
     */
    public static DateRange todayRange() {
        LocalDateTime now = now();
        LocalDateTime todayStart = todayStart();
        return new DateRange(todayStart, now);
    }

    /**
     * 최근 N일 시간 범위
     */
    public static DateRange lastDaysRange(int days) {
        LocalDateTime now = now();
        LocalDateTime start = now.minusDays(days);
        return new DateRange(start, now);
    }

    /**
     * 날짜 범위 클래스
     */
    public static class DateRange {
        private final LocalDateTime start;
        private final LocalDateTime end;

        public DateRange(LocalDateTime start, LocalDateTime end) {
            this.start = start;
            this.end = end;
        }

        public LocalDateTime getStart() {
            return start;
        }

        public LocalDateTime getEnd() {
            return end;
        }
    }
}


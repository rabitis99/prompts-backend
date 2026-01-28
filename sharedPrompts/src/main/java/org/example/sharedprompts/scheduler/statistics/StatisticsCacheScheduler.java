package org.example.sharedprompts.scheduler.statistics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.LockProviderToUse;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.example.sharedprompts.domain.statistics.service.StatisticsService;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 통계 캐시 갱신 스케줄러
 * - 주기적으로 통계 캐시를 갱신하여 최신 데이터 유지
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsCacheScheduler {

    private final StatisticsService statisticsService;
    private final CacheManager cacheManager;

    private static final String CACHE_NAME = "statistics";

    /**
     * 통계 캐시 갱신 (5분마다 실행)
     */
    @Scheduled(fixedRate = 5 * 60 * 1000L) // 5분
    @SchedulerLock(name = "StatisticsCacheScheduler", lockAtMostFor = "10m", lockAtLeastFor = "1m")
    @LockProviderToUse("fallbackLockProvider")
    public void refreshStatisticsCache() {
        log.info("StatisticsCacheScheduler started - refreshing statistics cache");

        try {
            // 캐시를 사용하지 않고 데이터 조회 (실패 시 기존 캐시 유지)
            statisticsService.getAllStatisticsWithoutCache();
            log.debug("Statistics data validation completed successfully");

            // 데이터 조회가 성공한 경우에만 캐시 초기화 후 재적재
            if (cacheManager.getCache(CACHE_NAME) != null) {
                cacheManager.getCache(CACHE_NAME).clear();
                log.debug("Statistics cache cleared after successful data validation");

                // 캐시 재생성 (getAllStatistics가 내부적으로 모든 통계를 조회)
                statisticsService.getAllStatistics();

                log.info("StatisticsCacheScheduler finished - cache refreshed successfully");
            }
        } catch (Exception e) {
            log.error("Failed to refresh statistics cache - keeping existing cache", e);
        }
    }
}

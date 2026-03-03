package org.example.sharedprompts.domain.statistics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.statistics.service.aicall.AiCallStatisticsService;
import org.example.sharedprompts.domain.statistics.service.prompt.PromptStatisticsService;
import org.example.sharedprompts.domain.statistics.service.user.UserStatisticsService;
import org.example.sharedprompts.domain.prompt.infrastructure.persistence.PromptRepository;
import org.example.sharedprompts.domain.prompt.infrastructure.persistence.UserStatisticsProjection;
import org.example.sharedprompts.dto.statistics.response.AiCallStatisticsResponseDto;
import org.example.sharedprompts.dto.statistics.response.MyStatisticsResponseDto;
import org.example.sharedprompts.dto.statistics.response.PromptStatisticsResponseDto;
import org.example.sharedprompts.dto.statistics.response.StatisticsResponseDto;
import org.example.sharedprompts.dto.statistics.response.UserStatisticsResponseDto;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 통계 서비스 구현체 (Facade)
 * 각 도메인별 통계 서비스를 조합하여 통합 통계를 제공합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsServiceImpl implements StatisticsService {

    private static final String CACHE_NAME = "statistics";

    private final UserStatisticsService userStatisticsService;
    private final PromptStatisticsService promptStatisticsService;
    private final AiCallStatisticsService aiCallStatisticsService;
    private final PromptRepository promptRepository;
    private final CacheManager cacheManager;

    @Override
    @Cacheable(value = CACHE_NAME, key = "'all'", unless = "#result == null")
    public StatisticsResponseDto getAllStatistics() {
        log.debug("전체 통계 조회");
        return StatisticsResponseDto.builder()
                .userStatistics(getUserStatistics())
                .promptStatistics(getPromptStatistics())
                .aiCallStatistics(getAiCallStatistics())
                .build();
    }

    @Override
    public UserStatisticsResponseDto getUserStatistics() {
        return userStatisticsService.getUserStatistics();
    }

    @Override
    public PromptStatisticsResponseDto getPromptStatistics() {
        return promptStatisticsService.getPromptStatistics();
    }

    @Override
    public AiCallStatisticsResponseDto getAiCallStatistics() {
        return aiCallStatisticsService.getAiCallStatistics();
    }

    /**
     * 캐시 갱신을 위한 전체 통계 강제 조회 메서드.
     */
    @Override
    public StatisticsResponseDto getAllStatisticsWithoutCache() {
        log.debug("전체 통계 조회 (캐시 evict 후 재조회)");
        
        // 관련 캐시 키를 임시로 evict하여 DB에서 직접 조회
        if (cacheManager.getCache(CACHE_NAME) != null) {
            Objects.requireNonNull(cacheManager.getCache(CACHE_NAME)).evictIfPresent("user");
            Objects.requireNonNull(cacheManager.getCache(CACHE_NAME)).evictIfPresent("prompt");
            Objects.requireNonNull(cacheManager.getCache(CACHE_NAME)).evictIfPresent("ai-call");
            Objects.requireNonNull(cacheManager.getCache(CACHE_NAME)).evictIfPresent("all");
        }
        
        // 캐시가 evict된 상태에서 조회 (DB에서 직접 조회됨)
        return StatisticsResponseDto.builder()
                .userStatistics(userStatisticsService.getUserStatistics())
                .promptStatistics(promptStatisticsService.getPromptStatistics())
                .aiCallStatistics(aiCallStatisticsService.getAiCallStatistics())
                .build();
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "'my:' + #userId.toString()", unless = "#result == null")
    public MyStatisticsResponseDto getMyStatistics(Long userId) {
        log.debug("개인 통계 조회: userId={}", userId);
        
        // 단일 쿼리로 사용자 통계 조회 (성능 최적화)
        // SUM은 결과가 없거나 null인 경우 null을 반환할 수 있으므로 Java에서 처리
        UserStatisticsProjection statistics = promptRepository.getUserStatisticsByUserId(userId);
        
        long myPromptsCount = 0L;
        long totalLikesReceived = 0L;
        
        if (statistics != null) {
            // COUNT는 항상 0 이상의 값을 반환하므로 null 체크는 선택적
            myPromptsCount = statistics.getPromptCount() != null ? statistics.getPromptCount() : 0L;
            // SUM은 null일 수 있으므로 반드시 null 체크 필요
            totalLikesReceived = statistics.getTotalLikeCount() != null ? statistics.getTotalLikeCount() : 0L;
        }
        
        return MyStatisticsResponseDto.builder()
                .myPromptsCount(myPromptsCount)
                .totalLikesReceived(totalLikesReceived)
                .build();
    }
}

package org.example.sharedprompts.domain.statistics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.statistics.service.aicall.AiCallStatisticsService;
import org.example.sharedprompts.domain.statistics.service.prompt.PromptStatisticsService;
import org.example.sharedprompts.domain.statistics.service.user.UserStatisticsService;
import org.example.sharedprompts.dto.statistics.response.AiCallStatisticsResponseDto;
import org.example.sharedprompts.dto.statistics.response.PromptStatisticsResponseDto;
import org.example.sharedprompts.dto.statistics.response.StatisticsResponseDto;
import org.example.sharedprompts.dto.statistics.response.UserStatisticsResponseDto;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 통계 서비스 구현체 (Facade)
 * 
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
}

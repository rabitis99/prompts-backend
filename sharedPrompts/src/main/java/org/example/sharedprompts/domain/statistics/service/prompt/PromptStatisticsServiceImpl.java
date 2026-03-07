package org.example.sharedprompts.domain.statistics.service.prompt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.infrastructure.persistence.PromptRepository;
import org.example.sharedprompts.domain.tag.repository.TagRepository;
import org.example.sharedprompts.domain.statistics.util.StatisticsDateUtils;
import org.example.sharedprompts.dto.statistics.response.PopularTagDto;
import org.example.sharedprompts.dto.statistics.response.PromptStatisticsResponseDto;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 프롬프트 통계 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromptStatisticsServiceImpl implements PromptStatisticsService {

    private static final String CACHE_NAME = "statistics";
    private static final int POPULAR_TAG_LIMIT = 10;
    private static final int MONTHLY_RANGE_DAYS = 30;

    private final PromptRepository promptRepository;
    private final TagRepository tagRepository;

    @Override
    @Cacheable(value = CACHE_NAME, key = "'prompt'", unless = "#result == null")
    public PromptStatisticsResponseDto getPromptStatistics() {
        log.debug("프롬프트 통계 조회");

        // 단일 기준 시점 사용 (모든 통계에서 일관된 시점 보장, UTC)
        LocalDateTime now = StatisticsDateUtils.now();
        LocalDateTime todayStart = StatisticsDateUtils.todayStart();
        LocalDateTime weekStart = now.minusDays(7);
        LocalDateTime monthStart = now.minusDays(MONTHLY_RANGE_DAYS);

        // 전체 프롬프트 수
        Long totalPrompts = promptRepository.count();

        // 오늘 생성된 프롬프트 수
        Long todayCreatedPrompts = promptRepository.countCreatedPromptsBetween(todayStart, now);

        // 최근 7일 생성된 프롬프트 수
        Long weeklyCreatedPrompts = promptRepository.countCreatedPromptsBetween(weekStart, now);

        // 최근 30일 생성된 프롬프트 수
        Long monthlyCreatedPrompts = promptRepository.countCreatedPromptsBetween(monthStart, now);

        // 전체 조회 수
        Long totalViewCount = getTotalViewCount();

        // 전체 좋아요 수
        Long totalLikeCount = getTotalLikeCount();

        // 인기 태그 목록 (상위 10개)
        List<PopularTagDto> popularTags = getPopularTags();

        return PromptStatisticsResponseDto.builder()
                .totalPrompts(totalPrompts)
                .todayCreatedPrompts(todayCreatedPrompts)
                .weeklyCreatedPrompts(weeklyCreatedPrompts)
                .monthlyCreatedPrompts(monthlyCreatedPrompts)
                .totalViewCount(totalViewCount)
                .totalLikeCount(totalLikeCount)
                .popularTags(popularTags)
                .build();
    }

    /**
     * 전체 조회 수 조회 (null 처리)
     */
    private Long getTotalViewCount() {
        Long totalViewCount = promptRepository.sumTotalViewCount();
        return totalViewCount != null ? totalViewCount : 0L;
    }

    /**
     * 전체 좋아요 수 조회 (null 처리)
     */
    private Long getTotalLikeCount() {
        Long totalLikeCount = promptRepository.sumTotalLikeCount();
        return totalLikeCount != null ? totalLikeCount : 0L;
    }

    /**
     * 인기 태그 목록 조회
     */
    private List<PopularTagDto> getPopularTags() {
        return tagRepository.findPopularTags(PageRequest.of(0, POPULAR_TAG_LIMIT))
                .stream()
                .map(tag -> PopularTagDto.builder()
                        .name(tag.getName())
                        .count(tag.getCount())
                        .build())
                .collect(Collectors.toList());
    }
}


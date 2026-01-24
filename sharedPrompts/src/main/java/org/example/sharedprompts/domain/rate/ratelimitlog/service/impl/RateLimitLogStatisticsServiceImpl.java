package org.example.sharedprompts.domain.rate.ratelimitlog.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.rate.ratelimitlog.enums.RateLimitType;
import org.example.sharedprompts.domain.rate.ratelimitlog.repository.RateLimitLogRepository;
import org.example.sharedprompts.domain.rate.ratelimitlog.service.RateLimitLogStatisticsService;
import org.example.sharedprompts.domain.rate.ratelimitlog.service.mapper.RateLimitLogStatisticsMapper;
import org.example.sharedprompts.dto.admin.response.RateLimitLogStatisticsResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.example.sharedprompts.domain.rate.ratelimitlog.constants.RateLimitLogConstants.Statistics;

/**
 * Rate Limit 로그 통계 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RateLimitLogStatisticsServiceImpl implements RateLimitLogStatisticsService {

    // RateLimitLogRepository는 CustomRateLimitLogRepository를 확장하므로
    // CustomRateLimitLogRepository의 메서드를 사용할 수 있습니다.
    private final RateLimitLogRepository rateLimitLogRepository;
    private final RateLimitLogStatisticsMapper mapper;

    @Override
    public RateLimitLogStatisticsResponseDto getStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        // RateLimitLogRepository는 CustomRateLimitLogRepository를 확장하므로
        // CustomRateLimitLogRepository의 메서드를 직접 사용할 수 있습니다.

        if (startDate == null || endDate == null) {
            throw new ApiException(ErrorCode.DATE_REQUIRED);
        }
        if (startDate.isAfter(endDate)) {
            throw new ApiException(ErrorCode.INVALID_DATE_RANGE);
        }

        // 통계 조회 기간 상한 검증
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween > Statistics.DEFAULT_STATISTICS_DAYS) {
            throw new ApiException(ErrorCode.STATISTICS_DATE_RANGE_EXCEEDED);
        }


        // 시간대별 통계
        Map<Integer, Long> hourlyStats = rateLimitLogRepository.countByHour(startDate, endDate);
        
        // 규칙별 통계
        Map<String, Long> ruleStats = rateLimitLogRepository.countByRuleName(startDate, endDate);
        
        // 타입별 통계
        Map<RateLimitType, Long> typeStats = rateLimitLogRepository.countByType(startDate, endDate);
        
        // 최다 위반 IP
        List<RateLimitLogStatisticsResponseDto.TopViolatorDto> topIps = mapper.toTopViolatorDtos(
                rateLimitLogRepository.findTopViolatingIps(startDate, endDate, Statistics.DEFAULT_TOP_VIOLATORS_LIMIT)
        );
        
        // 최다 위반 사용자
        List<RateLimitLogStatisticsResponseDto.TopViolatorDto> topUsers = mapper.toTopViolatorDtos(
                rateLimitLogRepository.findTopViolatingUsers(startDate, endDate, Statistics.DEFAULT_TOP_VIOLATORS_LIMIT)
        );
        
        return RateLimitLogStatisticsResponseDto.builder()
                .hourlyStats(hourlyStats)
                .ruleStats(ruleStats)
                .typeStats(typeStats)
                .topViolatingIps(topIps)
                .topViolatingUsers(topUsers)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }
}






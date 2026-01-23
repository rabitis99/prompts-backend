package org.example.sharedprompts.domain.rate.ratelimitlog.service.mapper;

import org.example.sharedprompts.dto.admin.response.RateLimitLogStatisticsResponseDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Rate Limit 로그 통계 매퍼
 * 
 * 통계 조회 결과를 DTO로 변환합니다.
 */
@Component
public class RateLimitLogStatisticsMapper {

    /**
     * 통계 결과를 TopViolatorDto 리스트로 변환합니다.
     * 
     * @param results 통계 조회 결과 (Object[] 배열 리스트)
     * @return TopViolatorDto 리스트
     */
    public List<RateLimitLogStatisticsResponseDto.TopViolatorDto> toTopViolatorDtos(List<Object[]> results) {
        return results.stream()
                .map(this::toTopViolatorDto)
                .collect(Collectors.toList());
    }

    /**
     * 단일 결과를 TopViolatorDto로 변환합니다.
     */
    private RateLimitLogStatisticsResponseDto.TopViolatorDto toTopViolatorDto(Object[] row) {
        return RateLimitLogStatisticsResponseDto.TopViolatorDto.builder()
                .identifier(String.valueOf(row[0]))
                .violationCount(((Number) row[1]).longValue())
                .build();
    }
}







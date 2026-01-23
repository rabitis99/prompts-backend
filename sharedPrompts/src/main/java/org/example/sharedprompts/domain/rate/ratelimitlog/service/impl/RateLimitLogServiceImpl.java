package org.example.sharedprompts.domain.rate.ratelimitlog.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.rate.ratelimitlog.RateLimitLog;
import org.example.sharedprompts.domain.rate.ratelimitlog.enums.RateLimitType;
import org.example.sharedprompts.domain.rate.ratelimitlog.repository.RateLimitLogRepository;
import org.example.sharedprompts.domain.rate.ratelimitlog.service.RateLimitLogService;
import org.example.sharedprompts.dto.admin.response.RateLimitLogResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Rate Limit 로그 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RateLimitLogServiceImpl implements RateLimitLogService {

    private final RateLimitLogRepository rateLimitLogRepository;

    @Override
    public Page<RateLimitLogResponseDto> getRateLimitLogs(
            Long userId,
            String clientIp,
            String ruleName,
            RateLimitType rateLimitType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    ) {
        Page<RateLimitLog> logs = rateLimitLogRepository.findByConditions(
                userId, clientIp, ruleName, rateLimitType, startDate, endDate, pageable
        );
        
        return logs.map(RateLimitLogResponseDto::from);
    }
}






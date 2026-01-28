package org.example.sharedprompts.domain.prompt.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.shared.service.BaseCountService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 프롬프트 조회수(usageCount) 관리 서비스 구현체
 * 
 * <p>Redis 키 형식: prompt:usage:{promptId}
 * <p>Redis 장애 시 예외를 발생시키지 않고 로그만 남깁니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptUsageCountServiceImpl implements PromptUsageCountService {

    private final BaseCountService baseCountService;

    /**
     * Redis 키 생성
     * 형식: prompt:usage:{promptId}
     */
    private String usageKey(Long promptId) {
        return "prompt:usage:" + promptId;
    }

    @Override
    public void incrementUsageCount(Long promptId) {
        try {
            baseCountService.increment(usageKey(promptId));
        } catch (Exception e) {
            // Redis 장애 시 조회 API는 실패하지 않도록 조용히 무시
            log.warn("Failed to increment usage count in Redis. promptId={}, error={}", 
                    promptId, e.getMessage());
        }
    }

    @Override
    public Map<Long, Long> getUsageCounts(List<Long> promptIds) {
        try {
            return baseCountService.getCounts(promptIds, this::usageKey);
        } catch (Exception e) {
            log.warn("Failed to get usage counts from Redis. promptIds={}, error={}", 
                    promptIds, e.getMessage());
            // Redis 장애 시 빈 맵 반환 (스케줄러에서 재시도 가능)
            return Map.of();
        }
    }
}


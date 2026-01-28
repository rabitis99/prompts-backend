package org.example.sharedprompts.domain.prompt.service;

import java.util.List;
import java.util.Map;

/**
 * 프롬프트 조회수(usageCount) 관리 서비스
 * 
 * <p>Redis에 조회수를 누적하고, 주기적으로 DB에 반영하는 구조를 담당합니다.
 * 조회 API는 readOnly 트랜잭션을 유지하며, 조회 직후 DB UPDATE는 발생하지 않습니다.
 */
public interface PromptUsageCountService {

    /**
     * 프롬프트 조회수 증가
     * Redis에만 반영하며, DB 업데이트는 스케줄러에서 주기적으로 수행합니다.
     * 
     * @param promptId 프롬프트 ID
     */
    void incrementUsageCount(Long promptId);

    /**
     * 여러 프롬프트의 조회수 조회
     * Redis에서 누적된 조회수를 조회합니다.
     * 
     * @param promptIds 프롬프트 ID 목록
     * @return 프롬프트 ID별 조회수 맵
     */
    Map<Long, Long> getUsageCounts(List<Long> promptIds);
}


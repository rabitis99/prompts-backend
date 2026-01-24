package org.example.sharedprompts.domain.rate.ratelimitlog.service;

import org.example.sharedprompts.domain.rate.ratelimitlog.RateLimitLog;

import java.util.List;

/**
 * Rate Limit 로그 배치 저장 서비스 인터페이스
 * 
 * 여러 로그를 배치로 저장하여 DB 부하를 감소시킵니다.
 * 높은 트래픽 환경에서 유용합니다.
 */
public interface RateLimitLogBatchService {

    /**
     * Rate Limit 로그를 배치로 저장합니다.
     * 
     * @param logs 저장할 로그 리스트
     */
    void saveBatch(List<RateLimitLog> logs);
}

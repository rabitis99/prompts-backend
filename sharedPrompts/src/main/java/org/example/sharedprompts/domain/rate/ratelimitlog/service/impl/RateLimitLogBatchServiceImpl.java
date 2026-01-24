package org.example.sharedprompts.domain.rate.ratelimitlog.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.rate.ratelimitlog.RateLimitLog;
import org.example.sharedprompts.domain.rate.ratelimitlog.repository.RateLimitLogRepository;
import org.example.sharedprompts.domain.rate.ratelimitlog.service.RateLimitLogBatchService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Rate Limit 로그 배치 저장 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitLogBatchServiceImpl implements RateLimitLogBatchService {

    private final RateLimitLogRepository repository;

    @Override
    @Async("rateLimitLogTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveBatch(List<RateLimitLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return;
        }
        List<RateLimitLog> snapshot = new ArrayList<>(logs);

        try {
            repository.saveAll(snapshot);
            log.debug("Saved {} rate limit logs in batch", snapshot.size());
        } catch (org.springframework.dao.DataAccessException e) {
            log.error("Database error while batch saving rate limit logs: count={}", logs.size(), e);
        } catch (IllegalArgumentException e) {
            log.error("Invalid argument while batch saving rate limit logs: count={}", logs.size(), e);
        } catch (Exception e) {
            log.error("Unexpected error while batch saving rate limit logs: count={}", logs.size(), e);
        }
    }
}






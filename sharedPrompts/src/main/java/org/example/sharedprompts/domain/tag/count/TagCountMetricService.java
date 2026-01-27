package org.example.sharedprompts.domain.tag.count;

import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 태그 카운트 업데이트 메트릭 서비스
 * - TagCountMetrics를 통한 중앙화된 메트릭 관리
 * - 성공/실패/재시도/처리시간 추적
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagCountMetricService {

    private final TagCountMetrics metrics;

    /**
     * 전체 성공 메트릭 증가
     */
    public void recordSuccess(int decreaseCount, int increaseCount) {
        metrics.getSuccessCounter().increment();
        metrics.getDecreaseCounter().increment(decreaseCount);
        metrics.getIncreaseCounter().increment(increaseCount);
    }

    /**
     * 태그별 성공 메트릭 기록
     * 
     * Note: tag_name 레이블은 높은 카디널리티 문제를 유발할 수 있어 제거했습니다.
     * 태그별 메트릭이 필요한 경우, 상위 N개 태그로 제한하거나 다른 방식으로 집계하세요.
     */
    public void recordTagSuccess(String operation) {
        if ("decrease".equals(operation)) {
            metrics.getDecreaseCounter().increment();
        } else if ("increase".equals(operation)) {
            metrics.getIncreaseCounter().increment();
        }
    }

    /**
     * 실패 메트릭 기록
     */
    public void recordFailure(String errorType) {
        metrics.recordFailure(errorType);
    }

    /**
     * 재시도 메트릭 기록
     */
    public void recordRetry(int retryCount) {
        metrics.recordRetry(retryCount);
    }

    /**
     * DLQ 메트릭 기록
     */
    public void recordDlq() {
        metrics.getDlqCounter().increment();
    }

    /**
     * 타이머 시작
     */
    public Timer.Sample startTimer() {
        return Timer.start(metrics.getMeterRegistry());
    }

    /**
     * 처리 시간 기록
     */
    public void recordDuration(Timer.Sample sample) {
        sample.stop(metrics.getDurationTimer());
    }
}


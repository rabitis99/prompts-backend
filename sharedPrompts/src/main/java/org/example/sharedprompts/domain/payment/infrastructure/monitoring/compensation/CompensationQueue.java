package org.example.sharedprompts.domain.payment.infrastructure.monitoring.compensation;

/**
 * 보상 트랜잭션 큐 인터페이스
 */
public interface CompensationQueue {

    /**
     * 보상 작업을 큐에 추가합니다.
     * @param task 보상 작업 정보
     */
    void enqueue(CompensationTask task);
}


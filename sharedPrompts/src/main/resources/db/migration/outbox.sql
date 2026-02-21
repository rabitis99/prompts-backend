-- ============================================
-- Transactional Outbox for Production Job Queue
-- ============================================
-- Job 생성과 동일 트랜잭션에서 outbox 행을 기록하고,
-- 별도 스케줄러가 RabbitMQ로 발행하여 발행 실패 시에도 재시도 가능하게 함.
-- ============================================

CREATE TABLE IF NOT EXISTS production_job_outbox (
    id BIGINT NOT NULL AUTO_INCREMENT,
    job_id VARCHAR(36) NOT NULL COMMENT 'Job ID (production_jobs.job_id)',
    max_retry_count INT NOT NULL DEFAULT 3 COMMENT '메시지 maxRetryCount',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '현재까지 발행 시도 횟수 (재시도 제한 적용용)',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, SENT, FAILED',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    sent_at DATETIME(6) NULL COMMENT '발행 완료 시각',
    error_message VARCHAR(500) NULL COMMENT '발행 실패 시 오류 메시지',
    PRIMARY KEY (id),
    INDEX idx_outbox_status_created (status, created_at),
    INDEX idx_outbox_job_id (job_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Job 큐 발행용 Transactional Outbox - 동일 TX 기록 후 비동기 발행';

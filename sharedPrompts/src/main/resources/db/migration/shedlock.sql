-- ============================================
-- ShedLock 테이블 생성 스크립트
-- ============================================
-- 
-- 용도: Redis 장애 시 DB 기반 LockProvider로 자동 전환하기 위한 테이블
-- 
-- 사용되는 스케줄러:
--   1. CommentCountSyncScheduler
--   2. PromptCountSyncScheduler
--   3. PromptLikeCountSyncScheduler
--   4. CommentLikeCountSyncScheduler
--   5. PromptFavoriteCountSyncScheduler
--   6. NotificationCleanupScheduler
--   7. RateLimitLogCleanupScheduler
--   8. StatisticsCacheScheduler
--
-- 테이블 구조:
--   - name: 스케줄러 이름 (Primary Key)
--   - lock_until: 락 만료 시간
--   - locked_at: 락 획득 시간
--   - locked_by: 락을 획득한 인스턴스 식별자
--
-- ============================================

CREATE TABLE IF NOT EXISTS shedlock (
                                        name VARCHAR(64) NOT NULL COMMENT '스케줄러 이름 (Primary Key)',
                                        lock_until TIMESTAMP(3) NOT NULL COMMENT '락 만료 시간',
                                        locked_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '락 획득 시간',
                                        locked_by VARCHAR(255) NOT NULL COMMENT '락을 획득한 인스턴스 식별자 (hostname:pid)',
                                        PRIMARY KEY (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ShedLock 분산 락 테이블 - Redis 장애 시 DB 기반 LockProvider 사용';

-- 인덱스 추가 (lock_until 기준 정리 작업 최적화)
-- 만료된 락을 빠르게 찾기 위한 인덱스
CREATE INDEX idx_shedlock_lock_until ON shedlock(lock_until);

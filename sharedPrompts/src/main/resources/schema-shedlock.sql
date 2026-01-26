-- ============================================
-- ShedLock 테이블 자동 생성 스크립트
-- ============================================
-- 
-- Spring Boot의 schema.sql 자동 실행 기능을 사용하여
-- 애플리케이션 시작 시 자동으로 테이블을 생성합니다.
--
-- 설정 방법:
--   application.yml에 다음 설정 추가:
--   spring:
--     sql:
--       init:
--         mode: always  # 또는 never (기본값: embedded)
--         schema-locations: classpath:schema-shedlock.sql
--
-- 주의사항:
--   - 프로덕션 환경에서는 수동으로 테이블을 생성하는 것을 권장
--   - 이 스크립트는 개발 환경에서만 사용하는 것을 권장
--
-- ============================================

CREATE TABLE IF NOT EXISTS shedlock (
                                        name VARCHAR(64) NOT NULL COMMENT '스케줄러 이름 (Primary Key)',
    lock_until TIMESTAMP(3) NOT NULL COMMENT '락 만료 시간',
    locked_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '락 획득 시간',
    locked_by VARCHAR(255) NOT NULL COMMENT '락을 획득한 인스턴스 식별자 (hostname:pid)',
    PRIMARY KEY (name)
    ,
    INDEX idx_shedlock_lock_until (lock_until)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    ,
    COMMENT='ShedLock 분산 락 테이블 - Redis 장애 시 DB 기반 LockProvider 사용';

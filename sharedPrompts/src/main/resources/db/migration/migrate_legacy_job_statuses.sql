-- ============================================
-- Legacy Job Status 마이그레이션 스크립트
-- ============================================
--
-- ⚠ Flyway가 관리하지 않는 수동 실행 전용 스크립트입니다.
--    Flyway 버전 명명 규칙(V<버전>__<설명>.sql)을 따르지 않으므로 자동 적용되지 않습니다.
--
-- 목적: 배포 시점에 비정상 상태로 남아있는 Job들을 복구 가능한 상태로 변환
--
-- 지원 상태 (v2: UNKNOWN, RETRYING 추가):
--   - Legacy 중간 상태: AI_CALLED, PARSED, RENDERED, STORED → PENDING/FAILED
--   - UNKNOWN 상태: timeout ambiguity 상태 → FAILED (운영자 수동 확인 권고)
--   - RETRYING 상태: retry 대기 중 재기동 시 → PENDING (재시도 가능하도록)
--
-- 실행 시점: 배포 전 또는 배포 직후 (애플리케이션 시작 전 권장)
--
-- 주의사항:
--   1. 프로덕션 환경에서는 배포 전에 먼저 실행하여 영향 범위 확인
--   2. 마이그레이션 전에 백업 권장
--   3. 마이그레이션 후 영향받은 Job 수를 확인하고 모니터링
--   4. 마이그레이션 완료 후 본 파일 하단의 "최종 검증 쿼리" 주석을 해제하여 실행해 성공 여부를 반드시 확인
--
-- ============================================

-- ============================================
-- 섹션 A: Legacy 중간 상태 처리 (AI_CALLED, PARSED, RENDERED, STORED 등)
-- ============================================
-- 단일 트랜잭션으로 실행하여 중간 실패 시 부분 마이그레이션 방지.
-- 실패 시 수동으로 ROLLBACK 후 원인 조치하고 재실행하세요.

START TRANSACTION;

-- A-1. 영향받을 Job 수 확인 (실제 마이그레이션 전에 실행)
-- SELECT
--     status,
--     COUNT(*) as count
-- FROM production_jobs
-- WHERE status IN ('AI_CALLED', 'PARSED', 'RENDERED', 'STORED')
-- GROUP BY status;

-- A-2. 최근 중간 상태 → PENDING 변환 (30분 이내, 재시도 가능)
-- error_message를 status 갱신 전에 설정하여 원래 상태가 기록되도록 함
UPDATE production_jobs
SET
    error_message = CONCAT('Legacy status migration: ', status, ' -> PENDING'),
    status = 'PENDING',
    started_at = NULL,
    completed_at = NULL,
    version = version + 1,
    updated_at = NOW()
WHERE status IN ('AI_CALLED', 'PARSED', 'RENDERED', 'STORED')
  AND (started_at IS NULL OR started_at >= DATE_SUB(NOW(), INTERVAL 30 MINUTE));

-- A-3. 오래된 중간 상태 → FAILED 변환 (30분 초과)
UPDATE production_jobs
SET
    error_message = CONCAT('Legacy status migration: ', status, ' -> FAILED (stale job)'),
    status = 'FAILED',
    completed_at = NOW(),
    version = version + 1,
    updated_at = NOW()
WHERE status IN ('AI_CALLED', 'PARSED', 'RENDERED', 'STORED')
  AND started_at IS NOT NULL
  AND started_at < DATE_SUB(NOW(), INTERVAL 30 MINUTE);

-- ============================================
-- 섹션 B: UNKNOWN 상태 처리 (P3-1 도입 이전 배포 시 잔존 가능)
-- ============================================
-- UNKNOWN = AI/S3 timeout으로 성공/실패 확정 불가 상태.
-- 배포 전 이미 UNKNOWN인 Job은 FAILED로 전이하고 운영자가 AI provider에서 직접 확인해야 함.
-- AI provider에 중복 호출하지 않도록 즉시 재처리 금지.

-- B-1. UNKNOWN Job 수 확인
-- SELECT COUNT(*) as unknown_count, MIN(updated_at) as oldest_unknown
-- FROM production_jobs WHERE status = 'UNKNOWN';

-- B-2. 10분 초과 UNKNOWN → FAILED (복구 불가, 운영자 수동 확인 권고)
--      updated_at < 10분 조건으로 "신선한" UNKNOWN은 이 UPDATE에서 제외됨.
--      제외된 UNKNOWN은 영구 방치되지 않음: JobRecoveryScheduler가 주기적으로
--      UnknownJobRecoveryHandler를 통해 UNKNOWN Job을 조회하며, startedAt 기준
--      stale 임계값(기본 30분, job.recovery.stale-job-threshold-minutes) 초과 시
--      FAILED로 전이하므로, 재기동 후 다음 스케줄 주기에 자동 복구됨.
UPDATE production_jobs
SET
    status = 'FAILED',
    completed_at = NOW(),
    error_message = CONCAT(
        'Migration: UNKNOWN state exceeded threshold. Manual verification required. Original: ',
        COALESCE(error_message, 'N/A')
    ),
    version = version + 1,
    updated_at = NOW()
WHERE status = 'UNKNOWN'
  AND updated_at < DATE_SUB(NOW(), INTERVAL 10 MINUTE);

-- ============================================
-- 섹션 C: RETRYING 상태 처리 (P2-2 도입)
-- ============================================
-- RETRYING = 메시지 레벨 retry 큐에 재발행된 상태.
-- 재기동 시 RETRYING 상태로 남아있는 Job은 retry 메시지 유실 가능성이 있으므로
-- PENDING으로 복구하여 JobRecoveryScheduler가 재투입하도록 한다.

-- C-1. RETRYING Job 수 확인
-- SELECT COUNT(*) as retrying_count FROM production_jobs WHERE status = 'RETRYING';

-- C-2. RETRYING → PENDING 변환 (재투입 가능하도록)
UPDATE production_jobs
SET
    status = 'PENDING',
    started_at = NULL,
    completed_at = NULL,
    error_message = 'Migration: RETRYING state reset to PENDING (server restart detected)',
    version = version + 1,
    updated_at = NOW()
WHERE status = 'RETRYING';

COMMIT;

-- ============================================
-- 최종 확인 쿼리
-- ============================================
-- SELECT
--     status,
--     COUNT(*) as count
-- FROM production_jobs
-- WHERE status IN ('AI_CALLED', 'PARSED', 'RENDERED', 'STORED', 'UNKNOWN', 'RETRYING')
-- GROUP BY status;
--
-- 결과가 0이면 마이그레이션 성공


-- production_jobs.production_id 컬럼 추가
-- Job 상태 응답에 production_id를 포함하기 위해 사용. N+1 없이 조회 가능.
-- Flyway 미사용 시 수동 실행. 실행 시점·담당: DEPLOYMENT_CHECKLIST.md §1 참고.

ALTER TABLE production_jobs
ADD COLUMN production_id BIGINT NULL COMMENT 'production_artifacts.id, 완료 시 설정' AFTER artifact_id;

-- 기존 완료된 Job: artifact_id가 production artifact id이므로 동일 값으로 보정
UPDATE production_jobs
SET production_id = CAST(artifact_id AS SIGNED)
WHERE artifact_id IS NOT NULL AND artifact_id REGEXP '^[0-9]+$' AND production_id IS NULL;

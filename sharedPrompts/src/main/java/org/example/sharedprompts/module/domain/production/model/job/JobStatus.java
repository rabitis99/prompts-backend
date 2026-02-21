package org.example.sharedprompts.module.domain.production.model.job;

/**
 * Job 처리 상태
 *
 * 설계 원칙:
 * - 프로세스와 결과는 분리한다
 * - 상태는 Job만 가진다
 *
 * P2-2: 재시도 중 상태 가시성을 위해 RETRYING 추가
 * P3-1: AI/S3 timeout ambiguity 처리를 위해 UNKNOWN 추가
 *        UNKNOWN은 즉시 재호출 금지 - UnknownJobRecoveryScheduler가 5분 주기로 복구 처리
 */
public enum JobStatus {
    /**
     * 대기 중 (JobQueue 등록 완료)
     */
    PENDING,

    /**
     * 재시도 대기 중 (메시지 레벨 retry queue에 재발행됨)
     * FAILED → RETRYING: 메시지 레벨 retry 발행 성공 시
     * RETRYING → PROCESSING: consumer가 retry 메시지 소비 시
     */
    RETRYING,

    /**
     * 처리 중 (Worker가 Job 획득하여 처리 중)
     */
    PROCESSING,

    /**
     * 성공 (Job이 성공적으로 완료됨)
     */
    SUCCEEDED,

    /**
     * 실패 (처리 중 오류 발생, 재시도 불가)
     */
    FAILED,

    /**
     * 상태 불명 (AI/S3 호출 timeout - 성공/실패 확정 불가)
     * PROCESSING → UNKNOWN: timeout 발생 시
     * UNKNOWN → SUCCEEDED: 복구 스케줄러가 AI provider에서 성공 확인 시
     * UNKNOWN → FAILED: 복구 스케줄러가 실패 확인 또는 최대 재조회 초과 시
     * 즉시 재처리 금지 - UnknownJobRecoveryScheduler가 5분 주기로 상태 조회
     */
    UNKNOWN
}

package org.example.sharedprompts.module.domain.production.model.job;

/**
 * Job 처리 상태
 * 
 */
public enum JobStatus {
    PENDING,         // 대기 중 (JobQueue 등록 완료)
    PROCESSING,      // 처리 중 (Worker가 Job 획득)
    AI_CALLED,       // AI 호출 완료 (rawResponse 저장 완료)
    PARSED,          // AI 응답 파싱 완료 (parsedResponse 저장 완료)
    RENDERED,        // 렌더링 완료 (사용자 요청 포맷으로 변환 완료)
    STORED,          // 파일 저장 완료
    COMPLETED,       // 완료 (전체 프로세스 완료)
    FAILED,          // 실패 (처리 중 오류 발생)
    PARSE_FAILED     // 파싱 실패 (AI 응답 파싱 불가)
}


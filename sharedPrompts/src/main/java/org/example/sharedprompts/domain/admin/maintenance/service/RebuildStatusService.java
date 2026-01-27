package org.example.sharedprompts.domain.admin.maintenance.service;

import org.example.sharedprompts.domain.admin.enums.MaintenanceJobStatus;
import org.example.sharedprompts.dto.admin.response.RebuildLikeCountsStatusResponseDto;

import java.time.LocalDateTime;

/**
 * 재빌드 작업 상태를 관리하는 서비스
 * 멀티 인스턴스 환경에서 상태를 공유하기 위해 Redis를 사용할 수 있습니다.
 */
public interface RebuildStatusService {
    
    /**
     * 상태 저장 (로컬 또는 글로벌)
     */
    void saveStatus(MaintenanceJobStatus status, LocalDateTime startedAt, 
                   LocalDateTime finishedAt, String errorMessage);
    
    /**
     * 상태 조회
     */
    RebuildLikeCountsStatusResponseDto getStatus();
    
    /**
     * 현재 상태가 RUNNING인지 확인
     */
    boolean isRunning();
    
    /**
     * 상태 초기화
     */
    void reset();
}


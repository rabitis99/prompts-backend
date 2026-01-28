package org.example.sharedprompts.domain.admin.maintenance.rebuild.util;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.admin.enums.MaintenanceJobStatus;
import org.example.sharedprompts.domain.admin.maintenance.rebuild.enums.RebuildFailureType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 로컬 메모리 기반 상태 관리 매니저
 */
@Getter
@Slf4j
@Component
public class LocalRebuildStatusManager {
    
    private volatile MaintenanceJobStatus status = MaintenanceJobStatus.IDLE;
    private volatile LocalDateTime startedAt;
    private volatile LocalDateTime finishedAt;
    private volatile String errorMessage;
    
    /**
     * 락 획득 및 상태 초기화
     */
    public synchronized boolean acquireLockAndUpdateStatus() {
        if (status == MaintenanceJobStatus.RUNNING) {
            return false;
        }
        
        status = MaintenanceJobStatus.RUNNING;
        startedAt = LocalDateTime.now();
        finishedAt = null;
        errorMessage = null;
        return true;
    }
    
    /**
     * 상태를 COMPLETED로 업데이트
     */
    public synchronized void updateStatusToCompleted() {
        status = MaintenanceJobStatus.COMPLETED;
        errorMessage = null;
    }
    
    /**
     * 상태를 FAILED로 업데이트
     * 예외 타입을 enum 기반으로 관리하여 타입 안전성 확보
     */
    public synchronized void updateStatusToFailed(Exception e) {
        status = MaintenanceJobStatus.FAILED;
        RebuildFailureType failureType = RebuildFailureType.from(e);
        errorMessage = failureType.buildErrorMessage(e);
    }
    
    /**
     * 완료 시간 업데이트
     */
    public synchronized void updateFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

}


package org.example.sharedprompts.domain.admin.maintenance.rebuild.util;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.admin.enums.MaintenanceJobStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 로컬 메모리 기반 상태 관리 매니저
 */
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
     */
    public synchronized void updateStatusToFailed(Exception e) {
        status = MaintenanceJobStatus.FAILED;
        errorMessage = e.getMessage() != null 
                ? e.getMessage() 
                : e.getClass().getSimpleName();
    }
    
    /**
     * 완료 시간 업데이트
     */
    public synchronized void updateFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }
    
    public MaintenanceJobStatus getStatus() {
        return status;
    }
    
    public LocalDateTime getStartedAt() {
        return startedAt;
    }
    
    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
}


package org.example.sharedprompts.domain.admin.maintenance.rebuild;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.admin.enums.MaintenanceJobStatus;
import org.example.sharedprompts.domain.admin.maintenance.service.RebuildStatusService;
import org.example.sharedprompts.dto.admin.response.RebuildLikeCountsStatusResponseDto;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 로컬 메모리 기반 상태 관리 서비스
 * 단일 인스턴스 환경에서 사용합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalRebuildStatusService implements RebuildStatusService {
    
    private volatile MaintenanceJobStatus status = MaintenanceJobStatus.IDLE;
    private volatile LocalDateTime startedAt;
    private volatile LocalDateTime finishedAt;
    private volatile String errorMessage;
    
    @Override
    public void saveStatus(MaintenanceJobStatus status, LocalDateTime startedAt, 
                          LocalDateTime finishedAt, String errorMessage) {
        this.status = status;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.errorMessage = errorMessage;
        log.debug("로컬 상태 저장 완료: {}", status);
    }
    
    @Override
    public RebuildLikeCountsStatusResponseDto getStatus() {
        return RebuildLikeCountsStatusResponseDto.builder()
                .status(status)
                .startedAt(startedAt)
                .finishedAt(finishedAt)
                .errorMessage(errorMessage)
                .build();
    }
    
    @Override
    public boolean isRunning() {
        return status == MaintenanceJobStatus.RUNNING;
    }
    
    @Override
    public void reset() {
        status = MaintenanceJobStatus.IDLE;
        startedAt = null;
        finishedAt = null;
        errorMessage = null;
        log.debug("로컬 상태 초기화 완료");
    }
}


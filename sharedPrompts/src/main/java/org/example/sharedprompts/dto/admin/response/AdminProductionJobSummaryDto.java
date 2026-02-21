package org.example.sharedprompts.dto.admin.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 관리자용 Production Job 요약 DTO (실패/UNKNOWN Job 모니터링)
 */
@Getter
@Builder
public class AdminProductionJobSummaryDto {

    private String jobId;
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;
    private Long userId;
    private Integer retryCount;
}

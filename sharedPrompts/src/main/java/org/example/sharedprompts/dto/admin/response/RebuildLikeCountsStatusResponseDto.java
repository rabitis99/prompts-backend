package org.example.sharedprompts.dto.admin.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

import org.example.sharedprompts.domain.admin.enums.MaintenanceJobStatus;

@Getter
@Builder
@AllArgsConstructor
public class RebuildLikeCountsStatusResponseDto {

    private final MaintenanceJobStatus status;
    private final LocalDateTime startedAt;
    private final LocalDateTime finishedAt;
    private final String errorMessage;
}




package org.example.sharedprompts.domain.admin.maintenance.rebuild.global;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.sharedprompts.domain.admin.enums.MaintenanceJobStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StatusData {
    public MaintenanceJobStatus status;
    public LocalDateTime startedAt;
    public LocalDateTime finishedAt;
    public String errorMessage;

    public StatusData(MaintenanceJobStatus status, LocalDateTime startedAt,
                      LocalDateTime finishedAt, String errorMessage) {
        this.status = status;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.errorMessage = errorMessage;
    }

}

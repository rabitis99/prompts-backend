package org.example.sharedprompts.dto.report.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.example.sharedprompts.domain.report.Report;
import org.example.sharedprompts.domain.report.enums.ReportReason;
import org.example.sharedprompts.domain.report.enums.ReportStatus;
import org.example.sharedprompts.domain.report.enums.ReportType;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ReportResponseDto {

    private Long id;
    @JsonProperty("report_type")
    private ReportType reportType;
    @JsonProperty("target_id")
    private Long targetId;
    private ReportReason reason;
    private String description;
    private ReportStatus status;
    @JsonProperty("reporter_id")
    private Long reporterId;
    @JsonProperty("reporter_nickname")
    private String reporterNickname;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    public static ReportResponseDto from(Report report) {
        return ReportResponseDto.builder()
                .id(report.getId())
                .reportType(report.getReportType())
                .targetId(report.getTargetId())
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus())
                .reporterId(report.getReporter().getId())
                .reporterNickname(report.getReporter().getNickname())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }
}


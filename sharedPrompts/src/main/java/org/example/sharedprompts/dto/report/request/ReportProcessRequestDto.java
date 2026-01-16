package org.example.sharedprompts.dto.report.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.report.Report;
import org.example.sharedprompts.domain.report.enums.ReportStatus;
import org.example.sharedprompts.domain.user.User;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportProcessRequestDto {

    @NotNull(message = "처리 상태는 필수입니다.")
    @ValidReportProcessStatus
    private ReportStatus status;

    @Size(max = 1000, message = "처리 코멘트는 최대 1000자까지 입력 가능합니다.")
    private String processComment; // 처리 코멘트 (선택)

    /**
     * Report 엔티티에 처리 정보 적용
     * @param report 신고 엔티티
     * @param processor 처리자 (관리자)
     */
    public void applyTo(Report report, User processor) {
        report.updateStatus(this.status, processor, this.processComment);
    }
}


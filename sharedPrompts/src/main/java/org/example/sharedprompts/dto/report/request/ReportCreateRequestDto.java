package org.example.sharedprompts.dto.report.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.comment.Comment;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.domain.report.Report;
import org.example.sharedprompts.domain.report.enums.ReportReason;
import org.example.sharedprompts.domain.report.enums.ReportStatus;
import org.example.sharedprompts.domain.report.enums.ReportType;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportCreateRequestDto {

    @NotNull(message = "신고 타입은 필수입니다.")
    private ReportType reportType;

    @NotNull(message = "신고 대상 ID는 필수입니다.")
    @Positive(message = "신고 대상 ID는 양수여야 합니다.")
    private Long targetId;

    @NotNull(message = "신고 사유는 필수입니다.")
    private ReportReason reason;

    @Size(max = 1000, message = "신고 상세 설명은 최대 1000자까지 입력 가능합니다.")
    private String description; // 신고 상세 설명 (선택)

    /**
     * Report 엔티티로 변환
     * @param prompt 프롬프트 엔티티 (reportType이 PROMPT인 경우)
     * @param comment 댓글 엔티티 (reportType이 COMMENT인 경우)
     * @param reporter 신고자
     * @return Report 엔티티
     * @throws ApiException reportType과 prompt/comment가 일치하지 않는 경우
     */
    public Report toEntity(Prompt prompt, Comment comment, User reporter) {
        // null 방어 검증
        if (this.reportType == null) {
            throw new ApiException(ErrorCode.REPORT_TYPE_REQUIRED);
        }
        if (reporter == null) {
            throw new ApiException(ErrorCode.REPORT_REPORTER_REQUIRED);
        }
        
        // reportType과 prompt/comment 일치 검증
        if (this.reportType == ReportType.PROMPT && prompt == null) {
            throw new ApiException(ErrorCode.REPORT_PROMPT_MISSING);
        }
        if (this.reportType == ReportType.COMMENT && comment == null) {
            throw new ApiException(ErrorCode.REPORT_COMMENT_MISSING);
        }
        if (this.reportType == ReportType.PROMPT && comment != null) {
            throw new ApiException(ErrorCode.REPORT_TARGET_CONFLICT);
        }
        if (this.reportType == ReportType.COMMENT && prompt != null) {
            throw new ApiException(ErrorCode.REPORT_TARGET_CONFLICT);
        }
        
        return Report.builder()
                .reportType(this.reportType)
                .prompt(prompt)
                .comment(comment)
                .reason(this.reason)
                .description(this.description)
                .status(ReportStatus.PENDING)
                .reporter(reporter)
                .build();
    }
}


package org.example.sharedprompts.domain.report;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.domain.comment.Comment;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.domain.report.enums.ReportReason;
import org.example.sharedprompts.domain.report.enums.ReportStatus;
import org.example.sharedprompts.domain.report.enums.ReportType;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.global.entity.BaseEntity;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
        name = "reports",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_report_prompt_reporter",
                        columnNames = {"prompt_id", "reporter_id"}
                ),
                @UniqueConstraint(
                        name = "uk_report_comment_reporter",
                        columnNames = {"comment_id", "reporter_id"}
                )
        },
        indexes = {
                // 추가된 인덱스 목록 (우선순위: 필수)
                // 대기 중인 신고를 최신순으로 조회
                @Index(name = "idx_reports_status_created_at", columnList = "status, created_at"),
                // 타입별 상태 필터링
                @Index(name = "idx_reports_type_status", columnList = "report_type, status"),
                // 사용자별 신고 조회
                @Index(name = "idx_reports_reporter", columnList = "reporter_id"),
                // 관리자가 처리한 신고 조회
                @Index(name = "idx_reports_processor", columnList = "processor_id"),
                // 관리자 대시보드용 복합 인덱스
                @Index(name = "idx_reports_type_status_created_at", columnList = "report_type, status, created_at")
        }
)
public class Report extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportType reportType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prompt_id")
    private Prompt prompt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private Comment comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportReason reason;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processor_id")
    private User processor;

    @Column(columnDefinition = "TEXT")
    private String processComment;

    // 낙관적 락
    @Version
    private Long version;

    /* ------------------------- */
    /*        도메인 메서드       */
    /* ------------------------- */

    public boolean canChangeStatus() {
        return this.status != ReportStatus.RESOLVED && this.status != ReportStatus.REJECTED;
    }

    public void updateStatus(ReportStatus status, User processor, String processComment) {
        this.status = status;
        this.processor = processor;
        this.processComment = processComment;
    }

    public Long getTargetId() {
        if (reportType == ReportType.PROMPT) return prompt != null ? prompt.getId() : null;
        if (reportType == ReportType.COMMENT) return comment != null ? comment.getId() : null;
        return null;
    }
}

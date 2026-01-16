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
    private Prompt prompt; // 프롬프트 신고인 경우

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private Comment comment; // 댓글 신고인 경우

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportReason reason;

    @Column(columnDefinition = "TEXT")
    private String description; // 신고 상세 설명

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter; // 신고자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processor_id")
    private User processor; // 처리자 (관리자)

    @Column(columnDefinition = "TEXT")
    private String processComment; // 처리 코멘트

    /**
     * 신고 상태 업데이트 (처리자 포함)
     * @param status 새로운 상태
     * @param processor 처리자 (관리자)
     * @param processComment 처리 코멘트
     */
    public void updateStatus(ReportStatus status, User processor, String processComment) {
        this.status = status;
        this.processor = processor;
        this.processComment = processComment;
    }

    /**
     * 신고 상태 업데이트 (처리자 없이)
     * @param status 새로운 상태
     */
    public void updateStatus(ReportStatus status) {
        this.status = status;
    }

    /**
     * 상태 변경 가능 여부 확인
     * @return 이미 최종 처리된 신고(RESOLVED, REJECTED)인 경우 false
     */
    public boolean canChangeStatus() {
        return this.status != ReportStatus.RESOLVED && this.status != ReportStatus.REJECTED;
    }

    /**
     * 신고 대상 ID 조회 (편의 메서드)
     * @return 신고 대상 ID, 타입과 엔티티가 일치하지 않으면 null
     */
    public Long getTargetId() {
        if (reportType == ReportType.PROMPT) {
            return prompt != null ? prompt.getId() : null;
        } else if (reportType == ReportType.COMMENT) {
            return comment != null ? comment.getId() : null;
        }
        return null;
    }

    /**
     * 신고 대상이 프롬프트인지 확인
     */
    public boolean isPromptReport() {
        return reportType == ReportType.PROMPT && prompt != null;
    }

    /**
     * 신고 대상이 댓글인지 확인
     */
    public boolean isCommentReport() {
        return reportType == ReportType.COMMENT && comment != null;
    }

}


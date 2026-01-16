package org.example.sharedprompts.domain.report.repository;

import org.example.sharedprompts.domain.comment.Comment;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.domain.report.Report;
import org.example.sharedprompts.domain.report.enums.ReportStatus;
import org.example.sharedprompts.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long>, CustomReportRepository {

    // 프롬프트와 신고자로 중복 신고 체크
    Optional<Report> findByPromptAndReporter(Prompt prompt, User reporter);

    // 댓글과 신고자로 중복 신고 체크
    Optional<Report> findByCommentAndReporter(Comment comment, User reporter);

    // 특정 프롬프트에 대한 모든 신고 조회
    List<Report> findByPrompt(Prompt prompt);

    // 특정 댓글에 대한 모든 신고 조회
    List<Report> findByComment(Comment comment);

    // 신고 상세 조회 (fetch join으로 N+1 문제 방지)
    @Query("""
        SELECT DISTINCT r FROM Report r
        LEFT JOIN FETCH r.reporter
        LEFT JOIN FETCH r.processor
        LEFT JOIN FETCH r.prompt
        LEFT JOIN FETCH r.comment
        WHERE r.id = :reportId
    """)
    Optional<Report> findByIdWithDetails(@Param("reportId") Long reportId);

    // 프롬프트와 상태로 신고 개수 조회
    long countByPromptAndStatus(Prompt prompt, ReportStatus status);

    // 댓글과 상태로 신고 개수 조회
    long countByCommentAndStatus(Comment comment, ReportStatus status);
}


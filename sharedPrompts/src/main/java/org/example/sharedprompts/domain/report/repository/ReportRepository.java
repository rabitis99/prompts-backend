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
    @Query("""
        SELECT DISTINCT r FROM Report r
        JOIN FETCH r.reporter
        LEFT JOIN FETCH r.processor
        LEFT JOIN FETCH r.prompt
        LEFT JOIN FETCH r.comment
        WHERE r.id = :reportId
    """)
    Optional<Report> findByIdWithDetails(@Param("reportId") Long reportId);

    long countByPromptAndStatus(Prompt prompt, ReportStatus status);

    long countByCommentAndStatus(Comment comment, ReportStatus status);
}


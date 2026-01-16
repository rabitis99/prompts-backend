package org.example.sharedprompts.domain.report.repository;

import org.example.sharedprompts.domain.report.Report;
import org.example.sharedprompts.domain.report.enums.ReportStatus;
import org.example.sharedprompts.domain.report.enums.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomReportRepository {
    Page<Report> findAllReports(Pageable pageable);
    Page<Report> findReportsByStatus(ReportStatus status, Pageable pageable);
    Page<Report> findReportsByType(ReportType reportType, Pageable pageable);
    Page<Report> findReportsByStatusAndType(ReportStatus status, ReportType reportType, Pageable pageable);
    Page<Report> findReportsByReporterId(Long reporterId, Pageable pageable);
}


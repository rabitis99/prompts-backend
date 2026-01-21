package org.example.sharedprompts.domain.admin.service;

import org.example.sharedprompts.domain.report.enums.ReportStatus;
import org.example.sharedprompts.dto.report.request.ReportProcessRequestDto;
import org.example.sharedprompts.dto.report.response.ReportDetailResponseDto;
import org.example.sharedprompts.dto.report.response.ReportResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminReportService {

    Page<ReportResponseDto> getReports(ReportStatus status, Pageable pageable);

    ReportDetailResponseDto getReportDetail(Long reportId);

    ReportDetailResponseDto processReport(Long reportId, Long adminId, ReportProcessRequestDto requestDto);
}



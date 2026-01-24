package org.example.sharedprompts.domain.admin.report.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.admin.validator.AdminValidator;
import org.example.sharedprompts.domain.report.enums.ReportStatus;
import org.example.sharedprompts.domain.report.service.ReportService;
import org.example.sharedprompts.dto.report.request.ReportProcessRequestDto;
import org.example.sharedprompts.dto.report.response.ReportDetailResponseDto;
import org.example.sharedprompts.dto.report.response.ReportResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminReportServiceImpl implements AdminReportService {

    private final ReportService reportService;
    private final AdminValidator adminValidator;

    private static final int MAX_PAGE_SIZE = 100;

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponseDto> getReports(ReportStatus status, Pageable pageable) {
        adminValidator.validatePageSize(pageable, MAX_PAGE_SIZE);
        if (status == null) {
            return reportService.getReports(pageable);
        }
        return reportService.getReportsByStatus(status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportDetailResponseDto getReportDetail(Long reportId) {
        return reportService.getReportDetail(reportId);
    }

    @Override
    @Transactional
    public ReportDetailResponseDto processReport(Long reportId, Long adminId, ReportProcessRequestDto requestDto) {
        return reportService.processReport(reportId, adminId, requestDto);
    }
}


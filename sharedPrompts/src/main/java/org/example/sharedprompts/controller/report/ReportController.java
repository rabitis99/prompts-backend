package org.example.sharedprompts.controller.report;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.domain.report.enums.ReportStatus;
import org.example.sharedprompts.domain.report.enums.ReportType;
import org.example.sharedprompts.domain.report.service.ReportService;
import org.example.sharedprompts.dto.report.request.ReportCreateRequestDto;
import org.example.sharedprompts.dto.report.request.ReportProcessRequestDto;
import org.example.sharedprompts.dto.report.response.ReportDetailResponseDto;
import org.example.sharedprompts.dto.report.response.ReportResponseDto;
import org.example.sharedprompts.global.response.CustomResponse;
import org.example.sharedprompts.global.response.CustomResponseHelper;
import org.example.sharedprompts.global.response.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * 신고 생성
     */
    @PostMapping
    public ResponseEntity<CustomResponse<ReportResponseDto>> createReport(
            @Valid @RequestBody ReportCreateRequestDto request,
            @CurrentUser AuthUser authUser
    ) {
        ReportResponseDto result = reportService.createReport(authUser.getId(), request);
        return CustomResponseHelper.created(result);
    }

    /**
     * 신고 상세 조회
     */
    @GetMapping("/{reportId}")
    public ResponseEntity<CustomResponse<ReportDetailResponseDto>> getReportDetail(
            @PathVariable Long reportId
    ) {
        return CustomResponseHelper.ok(reportService.getReportDetail(reportId));
    }

    /**
     * 신고 목록 조회 (전체)
     */
    @GetMapping
    public ResponseEntity<CustomResponse<PageResponse<ReportResponseDto>>> getReports(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<ReportResponseDto> reports = reportService.getReports(pageable);
        return CustomResponseHelper.ok(PageResponse.of(reports));
    }

    /**
     * 상태별 신고 목록 조회
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<CustomResponse<PageResponse<ReportResponseDto>>> getReportsByStatus(
            @PathVariable ReportStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<ReportResponseDto> reports = reportService.getReportsByStatus(status, pageable);
        return CustomResponseHelper.ok(PageResponse.of(reports));
    }

    /**
     * 타입별 신고 목록 조회
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<CustomResponse<PageResponse<ReportResponseDto>>> getReportsByType(
            @PathVariable ReportType type,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<ReportResponseDto> reports = reportService.getReportsByType(type, pageable);
        return CustomResponseHelper.ok(PageResponse.of(reports));
    }

    /**
     * 상태와 타입별 신고 목록 조회
     */
    @GetMapping("/status/{status}/type/{type}")
    public ResponseEntity<CustomResponse<PageResponse<ReportResponseDto>>> getReportsByStatusAndType(
            @PathVariable ReportStatus status,
            @PathVariable ReportType type,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<ReportResponseDto> reports = reportService.getReportsByStatusAndType(status, type, pageable);
        return CustomResponseHelper.ok(PageResponse.of(reports));
    }

    /**
     * 내가 신고한 목록 조회
     */
    @GetMapping("/me")
    public ResponseEntity<CustomResponse<PageResponse<ReportResponseDto>>> getMyReports(
            @CurrentUser AuthUser authUser,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<ReportResponseDto> reports = reportService.getMyReports(authUser.getId(), pageable);
        return CustomResponseHelper.ok(PageResponse.of(reports));
    }

    /**
     * 관리자 신고 처리
     */
    @PatchMapping("/{reportId}/process")
    public ResponseEntity<CustomResponse<ReportDetailResponseDto>> processReport(
            @PathVariable Long reportId,
            @Valid @RequestBody ReportProcessRequestDto request,
            @CurrentUser AuthUser authUser
    ) {
        ReportDetailResponseDto result = reportService.processReport(reportId, authUser.getId(), request);
        return CustomResponseHelper.ok(result);
    }
}


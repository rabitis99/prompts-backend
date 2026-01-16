package org.example.sharedprompts.domain.report.service;

import org.example.sharedprompts.domain.report.enums.ReportStatus;
import org.example.sharedprompts.domain.report.enums.ReportType;
import org.example.sharedprompts.dto.report.request.ReportCreateRequestDto;
import org.example.sharedprompts.dto.report.request.ReportProcessRequestDto;
import org.example.sharedprompts.dto.report.response.ReportDetailResponseDto;
import org.example.sharedprompts.dto.report.response.ReportResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReportService {

    /**
     * 신고 생성
     */
    ReportResponseDto createReport(Long userId, ReportCreateRequestDto requestDto);

    /**
     * 신고 상세 조회
     */
    ReportDetailResponseDto getReportDetail(Long reportId);

    /**
     * 신고 목록 조회 (전체)
     */
    Page<ReportResponseDto> getReports(Pageable pageable);

    /**
     * 상태별 신고 목록 조회
     */
    Page<ReportResponseDto> getReportsByStatus(ReportStatus status, Pageable pageable);

    /**
     * 타입별 신고 목록 조회
     */
    Page<ReportResponseDto> getReportsByType(ReportType reportType, Pageable pageable);

    /**
     * 상태와 타입별 신고 목록 조회
     */
    Page<ReportResponseDto> getReportsByStatusAndType(ReportStatus status, ReportType reportType, Pageable pageable);

    /**
     * 내가 신고한 목록 조회
     */
    Page<ReportResponseDto> getMyReports(Long userId, Pageable pageable);

    /**
     * 관리자 신고 처리
     */
    ReportDetailResponseDto processReport(Long reportId, Long adminId, ReportProcessRequestDto requestDto);

    /**
     * 특정 타겟에 대한 신고 개수 조회
     */
    long getReportCount(ReportType reportType, Long targetId, ReportStatus status);
}


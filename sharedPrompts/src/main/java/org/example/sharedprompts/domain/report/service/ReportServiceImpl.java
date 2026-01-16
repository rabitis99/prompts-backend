package org.example.sharedprompts.domain.report.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.comment.Comment;
import org.example.sharedprompts.domain.comment.repository.CommentRepository;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.domain.prompt.repository.PromptRepository;
import org.example.sharedprompts.domain.report.Report;
import org.example.sharedprompts.domain.report.enums.ReportStatus;
import org.example.sharedprompts.domain.report.enums.ReportType;
import org.example.sharedprompts.domain.report.repository.ReportRepository;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.enums.Role;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.dto.report.request.ReportCreateRequestDto;
import org.example.sharedprompts.dto.report.request.ReportProcessRequestDto;
import org.example.sharedprompts.dto.report.response.ReportDetailResponseDto;
import org.example.sharedprompts.dto.report.response.ReportResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PromptRepository promptRepository;
    private final CommentRepository commentRepository;

    @Override
    @Transactional
    public ReportResponseDto createReport(Long userId, ReportCreateRequestDto requestDto) {
        User reporter = getUser(userId);
        
        // 타겟 엔티티 조회 및 검증
        TargetEntity targetEntity = getTargetEntity(requestDto.getReportType(), requestDto.getTargetId());
        
        // 자기 자신의 콘텐츠 신고 방지
        validateNotSelfReport(targetEntity, reporter);
        
        // 중복 신고 체크 (같은 사용자가 같은 타겟을 이미 신고한 경우)
        checkDuplicateReport(targetEntity, reporter);

        Report report = requestDto.toEntity(targetEntity.prompt, targetEntity.comment, reporter);
        Report savedReport = reportRepository.save(report);
        return ReportResponseDto.from(savedReport);
    }
    
    /**
     * 타겟 엔티티 조회
     */
    private TargetEntity getTargetEntity(ReportType reportType, Long targetId) {
        if (reportType == ReportType.PROMPT) {
            Prompt prompt = promptRepository.findById(targetId)
                    .orElseThrow(() -> new ApiException(ErrorCode.PROMPT_NOT_FOUND));
            return new TargetEntity(prompt, null);
        } else if (reportType == ReportType.COMMENT) {
            Comment comment = commentRepository.findById(targetId)
                    .orElseThrow(() -> new ApiException(ErrorCode.COMMENT_NOT_FOUND));
            return new TargetEntity(null, comment);
        } else {
            throw new ApiException(ErrorCode.BAD_REQUEST);
        }
    }
    
    /**
     * 자기 자신의 콘텐츠 신고 방지
     */
    private void validateNotSelfReport(TargetEntity targetEntity, User reporter) {
        if (targetEntity.prompt != null && targetEntity.prompt.getAuthor().getId().equals(reporter.getId())) {
            throw new ApiException(ErrorCode.CANNOT_REPORT_OWN_CONTENT);
        }
        if (targetEntity.comment != null && targetEntity.comment.getUser().getId().equals(reporter.getId())) {
            throw new ApiException(ErrorCode.CANNOT_REPORT_OWN_CONTENT);
        }
    }
    
    /**
     * 중복 신고 체크
     */
    private void checkDuplicateReport(TargetEntity targetEntity, User reporter) {
        if (targetEntity.prompt != null) {
            reportRepository.findByPromptAndReporter(targetEntity.prompt, reporter)
                    .ifPresent(report -> {
                        throw new ApiException(ErrorCode.REPORT_ALREADY_EXISTS);
                    });
        } else if (targetEntity.comment != null) {
            reportRepository.findByCommentAndReporter(targetEntity.comment, reporter)
                    .ifPresent(report -> {
                        throw new ApiException(ErrorCode.REPORT_ALREADY_EXISTS);
                    });
        }
    }
    
    /**
     * 타겟 엔티티를 담는 내부 클래스
     */
    private static class TargetEntity {
        final Prompt prompt;
        final Comment comment;
        
        TargetEntity(Prompt prompt, Comment comment) {
            this.prompt = prompt;
            this.comment = comment;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ReportDetailResponseDto getReportDetail(Long reportId) {
        Report report = getReportWithDetails(reportId);
        return ReportDetailResponseDto.from(report);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponseDto> getReports(Pageable pageable) {
        Page<Report> reports = reportRepository.findAllReports(pageable);
        return reports.map(ReportResponseDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponseDto> getReportsByStatus(ReportStatus status, Pageable pageable) {
        Page<Report> reports = reportRepository.findReportsByStatus(status, pageable);
        return reports.map(ReportResponseDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponseDto> getReportsByType(ReportType reportType, Pageable pageable) {
        Page<Report> reports = reportRepository.findReportsByType(reportType, pageable);
        return reports.map(ReportResponseDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponseDto> getReportsByStatusAndType(ReportStatus status, ReportType reportType, Pageable pageable) {
        Page<Report> reports = reportRepository.findReportsByStatusAndType(status, reportType, pageable);
        return reports.map(ReportResponseDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponseDto> getMyReports(Long userId, Pageable pageable) {
        Page<Report> reports = reportRepository.findReportsByReporterId(userId, pageable);
        return reports.map(ReportResponseDto::from);
    }

    @Override
    @Transactional
    public ReportDetailResponseDto processReport(Long reportId, Long adminId, ReportProcessRequestDto requestDto) {
        User admin = validateAdmin(adminId);
        Report report = getReportWithDetails(reportId);
        
        // 이미 처리된 신고인지 확인 (Entity의 validateStatusChange와 중복이지만, 
        // 더 명확한 에러 메시지를 위해 Service에서도 체크)
        validateReportCanBeProcessed(report);

        requestDto.applyTo(report, admin);

        return ReportDetailResponseDto.from(report);
    }
    
    /**
     * 신고 처리 가능 여부 검증
     */
    private void validateReportCanBeProcessed(Report report) {
        if (report.getStatus() != ReportStatus.PENDING && report.getStatus() != ReportStatus.PROCESSING) {
            throw new ApiException(ErrorCode.REPORT_ALREADY_PROCESSED);
        }
    }
    
    /**
     * 관리자 권한 검증
     */
    private User validateAdmin(Long adminId) {
        User admin = getUser(adminId);
        if (admin.getRole() != Role.ROLE_ADMIN) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        return admin;
    }
    
    /**
     * 신고 상세 조회 (fetch join 포함)
     */
    private Report getReportWithDetails(Long reportId) {
        return reportRepository.findByIdWithDetails(reportId)
                .orElseThrow(() -> new ApiException(ErrorCode.REPORT_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public long getReportCount(ReportType reportType, Long targetId, ReportStatus status) {
        // 엔티티 조회 후 카운트 (Repository 메서드가 엔티티를 받도록 설계됨)
        // getTargetEntity에서 이미 존재 여부 검증을 수행하므로 중복 검증 제거
        TargetEntity targetEntity = getTargetEntity(reportType, targetId);
        
        if (targetEntity.prompt != null) {
            return reportRepository.countByPromptAndStatus(targetEntity.prompt, status);
        } else if (targetEntity.comment != null) {
            return reportRepository.countByCommentAndStatus(targetEntity.comment, status);
        } else {
            throw new ApiException(ErrorCode.BAD_REQUEST);
        }
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }
}


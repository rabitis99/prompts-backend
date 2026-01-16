package org.example.sharedprompts.domain.report.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.comment.Comment;
import org.example.sharedprompts.domain.comment.repository.CommentRepository;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.domain.prompt.repository.PromptRepository;
import org.example.sharedprompts.domain.report.Report;
import org.example.sharedprompts.domain.report.ReportTargetEntity;
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PromptRepository promptRepository;
    private final CommentRepository commentRepository;
    private final EntityManager entityManager;

    // ======================
    //      신고 생성
    // ======================
    @Override
    @Transactional
    public ReportResponseDto createReport(Long userId, ReportCreateRequestDto requestDto) {
        User reporter = getUser(userId);

        ReportTargetEntity target = getTargetEntity(requestDto.getReportType(), requestDto.getTargetId());
        validateNotSelfReport(target, reporter);
        validateReportTarget(requestDto.getReportType(), target);

        Report report = requestDto.toEntity(target.getPrompt(), target.getComment(), reporter);
        Report saved = reportRepository.save(report);

        return ReportResponseDto.from(saved);
    }

    // ======================
    //      신고 조회
    // ======================
    @Override
    @Transactional(readOnly = true)
    public ReportDetailResponseDto getReportDetail(Long reportId) {
        return ReportDetailResponseDto.from(getReportWithDetails(reportId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponseDto> getReports(Pageable pageable) {
        return reportRepository.findAllReports(pageable).map(ReportResponseDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponseDto> getReportsByStatus(ReportStatus status, Pageable pageable) {
        return reportRepository.findReportsByStatus(status, pageable)
                .map(ReportResponseDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponseDto> getReportsByType(ReportType reportType, Pageable pageable) {
        return reportRepository.findReportsByType(reportType, pageable)
                .map(ReportResponseDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponseDto> getReportsByStatusAndType(ReportStatus status, ReportType reportType, Pageable pageable) {
        return reportRepository.findReportsByStatusAndType(status, reportType, pageable)
                .map(ReportResponseDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponseDto> getMyReports(Long userId, Pageable pageable) {
        return reportRepository.findReportsByReporterId(userId, pageable)
                .map(ReportResponseDto::from);
    }

    @Override
    @Transactional
    public ReportDetailResponseDto processReport(Long reportId, Long adminId, ReportProcessRequestDto requestDto) {
        User admin = validateAdmin(adminId);
        Report report = getReportWithDetails(reportId);

        if (!report.canChangeStatus()) {
            throw new ApiException(ErrorCode.REPORT_ALREADY_PROCESSED);
        }

        requestDto.applyTo(report, admin);

        // 낙관적 락 적용: flush로 충돌 감지
        try {
            entityManager.flush();
        } catch (OptimisticLockException e) {
            throw new ApiException(ErrorCode.REPORT_ALREADY_PROCESSED);
        }

        return ReportDetailResponseDto.from(report);
    }

    // ======================
    //      신고 카운트
    // ======================
    @Override
    @Transactional(readOnly = true)
    public long getReportCount(ReportType reportType, Long targetId, ReportStatus status) {
        ReportTargetEntity target = getTargetEntity(reportType, targetId);

        if (target.getPrompt() != null) {
            return reportRepository.countByPromptAndStatus(target.getPrompt(), status);
        } else if (target.getComment() != null) {
            return reportRepository.countByCommentAndStatus(target.getComment(), status);
        } else {
            throw new ApiException(ErrorCode.BAD_REQUEST);
        }
    }

    // ======================
    //      내부 헬퍼 메서드
    // ======================
    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }

    private Report getReportWithDetails(Long reportId) {
        return reportRepository.findByIdWithDetails(reportId)
                .orElseThrow(() -> new ApiException(ErrorCode.REPORT_NOT_FOUND));
    }

    private User validateAdmin(Long adminId) {
        User admin = getUser(adminId);
        if (admin.getRole() != Role.ROLE_ADMIN) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        return admin;
    }

    private ReportTargetEntity getTargetEntity(ReportType reportType, Long targetId) {
        switch (reportType) {
            case PROMPT -> {
                Prompt prompt = promptRepository.findById(targetId)
                        .orElseThrow(() -> new ApiException(ErrorCode.PROMPT_NOT_FOUND));
                return new ReportTargetEntity(prompt, null);
            }
            case COMMENT -> {
                Comment comment = commentRepository.findById(targetId)
                        .orElseThrow(() -> new ApiException(ErrorCode.COMMENT_NOT_FOUND));
                return new ReportTargetEntity(null, comment);
            }
            default -> throw new ApiException(ErrorCode.BAD_REQUEST);
        }
    }

    private void validateNotSelfReport(ReportTargetEntity target, User reporter) {
        if (target.getPrompt() != null && target.getPrompt().getAuthor().getId().equals(reporter.getId())) {
            throw new ApiException(ErrorCode.CANNOT_REPORT_OWN_CONTENT);
        }
        if (target.getComment() != null && target.getComment().getUser().getId().equals(reporter.getId())) {
            throw new ApiException(ErrorCode.CANNOT_REPORT_OWN_CONTENT);
        }
    }

    private void validateReportTarget(ReportType reportType, ReportTargetEntity target) {
        boolean hasPrompt = target.getPrompt() != null;
        boolean hasComment = target.getComment() != null;

        if (hasPrompt == hasComment) {
            throw new ApiException(ErrorCode.REPORT_TARGET_CONFLICT);
        }
        if (reportType == ReportType.PROMPT && !hasPrompt) {
            throw new ApiException(ErrorCode.REPORT_PROMPT_MISSING);
        }
        if (reportType == ReportType.COMMENT && !hasComment) {
            throw new ApiException(ErrorCode.REPORT_COMMENT_MISSING);
        }
    }

}

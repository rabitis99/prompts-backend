package org.example.sharedprompts.domain.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.admin.util.AdminEntityFinder;
import org.example.sharedprompts.domain.admin.validator.AdminValidator;
import org.example.sharedprompts.domain.audit.AuditLog;
import org.example.sharedprompts.domain.audit.enums.AuditAction;
import org.example.sharedprompts.domain.audit.enums.AuditEntityType;
import org.example.sharedprompts.domain.audit.service.AuditLogService;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.dto.audit.response.AuditLogResponseDto;
import org.example.sharedprompts.domain.prompt.repository.PromptRepository;
import org.example.sharedprompts.domain.report.enums.ReportStatus;
import org.example.sharedprompts.domain.report.service.ReportService;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.enums.Role;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import org.example.sharedprompts.dto.admin.request.PromptVisibilityRequestDto;
import org.example.sharedprompts.dto.admin.request.UserBlockRequestDto;
import org.example.sharedprompts.dto.admin.request.UserRoleChangeRequestDto;
import org.example.sharedprompts.dto.admin.response.AdminPromptResponseDto;
import org.example.sharedprompts.dto.admin.response.AdminUserResponseDto;
import org.example.sharedprompts.dto.report.request.ReportProcessRequestDto;
import org.example.sharedprompts.dto.report.response.ReportDetailResponseDto;
import org.example.sharedprompts.dto.report.response.ReportResponseDto;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.jwt.service.UserTokenInvalidationService;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final PromptRepository promptRepository;
    private final ReportService reportService;
    private final AdminValidator adminValidator;
    private final AdminEntityFinder entityFinder;
    private final AuditLogService auditLogService;
    private final UserTokenInvalidationService tokenInvalidationService;

    // ======================
    //      사용자 관리
    // ======================

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserResponseDto> getUsers(Pageable pageable) {
        adminValidator.validatePageSize(pageable, 100);
        return userRepository.findAll(pageable)
                .map(AdminUserResponseDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserResponseDto> searchUsers(String keyword, Pageable pageable) {
        adminValidator.validatePageSize(pageable, 100);
        return userRepository.searchUsers(keyword, pageable)
                .map(AdminUserResponseDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponseDto getUser(Long userId) {
        User user = entityFinder.findUserById(userId);
        return AdminUserResponseDto.from(user);
    }

    @Override
    @Transactional
    public AdminUserResponseDto blockUser(Long userId, Long adminId, UserBlockRequestDto requestDto) {
        User user = entityFinder.findUserById(userId);
        
        adminValidator.validateNotSelf(adminId, userId, ErrorCode.CANNOT_MODIFY_SELF);
        adminValidator.validateNotAdmin(user, ErrorCode.CANNOT_BLOCK_ADMIN);

        if (Boolean.TRUE.equals(requestDto.getBlocked())) {
            user.block();
            log.info("사용자 차단: userId={}, adminId={}", userId, adminId);
            tokenInvalidationService.invalidateTokensOnBlock(userId);
        } else {
            user.unblock();
            log.info("사용자 차단 해제: userId={}, adminId={}", userId, adminId);
            tokenInvalidationService.invalidateTokensOnUnblock(userId);
        }

        return AdminUserResponseDto.from(user);
    }

    @Override
    @Transactional
    public AdminUserResponseDto changeUserRole(Long userId, Long adminId, UserRoleChangeRequestDto requestDto) {
        User user = entityFinder.findUserById(userId);
        
        adminValidator.validateNotSelf(adminId, userId, ErrorCode.CANNOT_MODIFY_SELF);
        adminValidator.validateNotAdmin(user, ErrorCode.CANNOT_CHANGE_ADMIN_ROLE);

        Role oldRole = user.getRole();
        adminValidator.validateNotSameRole(oldRole, requestDto.getRole());
        adminValidator.validateRoleChange(oldRole, requestDto.getRole());

        user.changeRole(requestDto.getRole());
        log.info("사용자 권한 변경: userId={}, oldRole={}, newRole={}, adminId={}", 
                userId, oldRole, requestDto.getRole(), adminId);

        tokenInvalidationService.invalidateTokensOnRoleChange(userId);

        return AdminUserResponseDto.from(user);
    }

    // ======================
    //      프롬프트 관리
    // ======================

    @Override
    @Transactional(readOnly = true)
    public Page<AdminPromptResponseDto> getPrompts(Pageable pageable) {
        adminValidator.validatePageSize(pageable, 100);
        return promptRepository.findAll(pageable)
                .map(AdminPromptResponseDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminPromptResponseDto> searchPrompts(String keyword, Pageable pageable) {
        adminValidator.validatePageSize(pageable, 100);
        if (keyword == null || keyword.trim().isEmpty()) {
            return getPrompts(pageable);
        }

        return promptRepository.searchPrompts(keyword, pageable)
                .map(AdminPromptResponseDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminPromptResponseDto getPrompt(Long promptId) {
        Prompt prompt = entityFinder.findPromptById(promptId);
        return AdminPromptResponseDto.from(prompt);
    }

    @Override
    @Transactional
    public void deletePrompt(Long promptId, Long adminId) {
        Prompt prompt = entityFinder.findPromptById(promptId);
        
        promptRepository.delete(prompt);
        log.info("프롬프트 삭제: promptId={}, adminId={}", promptId, adminId);
    }

    @Override
    @Transactional
    public AdminPromptResponseDto togglePromptVisibility(Long promptId, Long adminId, PromptVisibilityRequestDto requestDto) {
        Prompt prompt = entityFinder.findPromptById(promptId);
        
        boolean oldVisibility = prompt.isPublic();
        adminValidator.validateNotSameVisibility(oldVisibility, requestDto.getIsPublic());
        
        prompt.updateIsPublic(requestDto.getIsPublic());
        
        log.info("프롬프트 공개 상태 변경: promptId={}, oldVisibility={}, newVisibility={}, adminId={}", 
                promptId, oldVisibility, requestDto.getIsPublic(), adminId);
        
        return AdminPromptResponseDto.from(prompt);
    }

    // ======================
    //      신고 처리
    // ======================

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponseDto> getReports(ReportStatus status, Pageable pageable) {
        adminValidator.validatePageSize(pageable, 100);
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

    // ======================
    //      감사 로그 조회
    // ======================

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDto> getAuditLogs(
            Long actorId,
            AuditEntityType entityType,
            AuditAction action,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    ) {
        adminValidator.validateDateRange(startDate, endDate);
        adminValidator.validatePageSize(pageable, 100);
        
        Page<AuditLog> logs = auditLogService.getLogsWithFilters(
                actorId, entityType, action, startDate, endDate, pageable
        );
        return logs.map(AuditLogResponseDto::from);
    }

}


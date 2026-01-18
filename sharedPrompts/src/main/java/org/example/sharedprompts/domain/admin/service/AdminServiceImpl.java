package org.example.sharedprompts.domain.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.admin.util.AdminAuditLogger;
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
import org.example.sharedprompts.global.exception.ApiException;
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
    private final AdminAuditLogger adminAuditLogger;
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
        String trimmedKeyword = normalizeKeyword(keyword);
        if (trimmedKeyword == null || trimmedKeyword.isEmpty()) {
            return getUsers(pageable);
        }
        return userRepository.searchUsers(trimmedKeyword, pageable)
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
        User admin = entityFinder.findUserById(adminId);
        
        adminValidator.validateNotSelf(adminId, userId, ErrorCode.CANNOT_MODIFY_SELF);
        adminValidator.validateNotAdmin(user, ErrorCode.CANNOT_BLOCK_ADMIN);

        Boolean blocked = requestDto.getBlocked();
        if (blocked == null) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }
        
        if (blocked) {
            user.block();
            log.info("사용자 차단: userId={}, adminId={}", userId, adminId);
            tokenInvalidationService.invalidateTokensOnBlock(userId);
        } else {
            user.unblock();
            log.info("사용자 차단 해제: userId={}, adminId={}", userId, adminId);
            tokenInvalidationService.invalidateTokensOnUnblock(userId);
        }

        adminAuditLogger.logUserBlock(admin, userId, blocked);

        return AdminUserResponseDto.from(user);
    }

    @Override
    @Transactional
    public AdminUserResponseDto changeUserRole(Long userId, Long adminId, UserRoleChangeRequestDto requestDto) {
        User user = entityFinder.findUserById(userId);
        
        adminValidator.validateNotSelf(adminId, userId, ErrorCode.CANNOT_MODIFY_SELF);

        Role oldRole = user.getRole();
        adminValidator.validateNotSameRole(oldRole, requestDto.getRole());

        // 관리자에서 일반 사용자로 변경하는 경우 조건부 업데이트로 원자성 보장
        if (oldRole == Role.ROLE_ADMIN && requestDto.getRole() != Role.ROLE_ADMIN) {
            int updatedRows = userRepository.changeRoleFromAdminIfNotLast(userId, Role.ROLE_ADMIN, requestDto.getRole());
            if (updatedRows == 0) {
                throw new ApiException(ErrorCode.LAST_ADMIN_CANNOT_BE_MODIFIED);
            } else {
                // 조건부 업데이트 성공 - 엔티티 새로고침 필요
                userRepository.flush();
                user = entityFinder.findUserById(userId);
            }
        } else {
            // 일반 사용자 권한 변경 또는 관리자로 승격은 기존 방식 사용
            user.changeRole(requestDto.getRole());
        }

        log.info("사용자 권한 변경: userId={}, oldRole={}, newRole={}, adminId={}", 
                userId, oldRole, requestDto.getRole(), adminId);

        tokenInvalidationService.invalidateTokensOnRoleChange(userId);

        User admin = entityFinder.findUserById(adminId);
        adminAuditLogger.logUserRoleChange(admin, userId, oldRole.name(), requestDto.getRole().name());

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
        String trimmedKeyword = normalizeKeyword(keyword);
        if (trimmedKeyword == null || trimmedKeyword.isEmpty()) {
            return getPrompts(pageable);
        }

        return promptRepository.searchPromptsForAdmin(trimmedKeyword, pageable)
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

        User admin = entityFinder.findUserById(adminId);
        adminAuditLogger.logPromptDelete(admin, promptId);
    }

    @Override
    @Transactional
    public AdminPromptResponseDto togglePromptVisibility(Long promptId, Long adminId, PromptVisibilityRequestDto requestDto) {
        Prompt prompt = entityFinder.findPromptById(promptId);
        
        Boolean isPublic = requestDto.getIsPublic();
        if (isPublic == null) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }
        
        boolean oldVisibility = prompt.isPublic();
        adminValidator.validateNotSameVisibility(oldVisibility, isPublic);
        
        prompt.updateIsPublic(isPublic);
        
        log.info("프롬프트 공개 상태 변경: promptId={}, oldVisibility={}, newVisibility={}, adminId={}", 
                promptId, oldVisibility, isPublic, adminId);

        User admin = entityFinder.findUserById(adminId);
        adminAuditLogger.logPromptVisibilityChange(admin, promptId, isPublic);
        
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

    /**
     * 검색 키워드 정규화 (trim 및 null 처리)
     */
    private String normalizeKeyword(String keyword) {
        return keyword == null ? null : keyword.trim();
    }
}


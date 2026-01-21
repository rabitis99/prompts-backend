package org.example.sharedprompts.domain.admin.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.audit.enums.AuditAction;
import org.example.sharedprompts.domain.audit.enums.AuditEntityType;
import org.example.sharedprompts.domain.follow.FollowStatus;
import org.example.sharedprompts.dto.audit.response.AuditLogResponseDto;
import org.example.sharedprompts.domain.report.enums.ReportStatus;
import org.example.sharedprompts.dto.admin.request.PromptVisibilityRequestDto;
import org.example.sharedprompts.dto.admin.request.UserBlockRequestDto;
import org.example.sharedprompts.dto.admin.request.UserRoleChangeRequestDto;
import org.example.sharedprompts.dto.admin.response.AdminPromptResponseDto;
import org.example.sharedprompts.dto.admin.response.AdminUserResponseDto;
import org.example.sharedprompts.dto.report.request.ReportProcessRequestDto;
import org.example.sharedprompts.dto.report.response.ReportDetailResponseDto;
import org.example.sharedprompts.dto.report.response.ReportResponseDto;
import org.example.sharedprompts.dto.user.response.UserResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminUserService adminUserService;
    private final AdminPromptService adminPromptService;
    private final AdminReportService adminReportService;
    private final AdminAuditService adminAuditService;
    private final AdminFollowService adminFollowService;

    // ======================
    //      사용자 관리
    // ======================

    @Override
    public Page<AdminUserResponseDto> getUsers(Pageable pageable) {
        return adminUserService.getUsers(pageable);
    }

    @Override
    public Page<AdminUserResponseDto> searchUsers(String keyword, Pageable pageable) {
        return adminUserService.searchUsers(keyword, pageable);
    }

    @Override
    public AdminUserResponseDto getUser(Long userId) {
        return adminUserService.getUser(userId);
    }

    @Override
    public AdminUserResponseDto blockUser(Long userId, Long adminId, UserBlockRequestDto requestDto) {
        return adminUserService.blockUser(userId, adminId, requestDto);
    }

    @Override
    public AdminUserResponseDto changeUserRole(Long userId, Long adminId, UserRoleChangeRequestDto requestDto) {
        return adminUserService.changeUserRole(userId, adminId, requestDto);
    }

    // ======================
    //      프롬프트 관리
    // ======================

    @Override
    public Page<AdminPromptResponseDto> getPrompts(Pageable pageable) {
        return adminPromptService.getPrompts(pageable);
    }

    @Override
    public Page<AdminPromptResponseDto> searchPrompts(String keyword, Pageable pageable) {
        return adminPromptService.searchPrompts(keyword, pageable);
    }

    @Override
    public AdminPromptResponseDto getPrompt(Long promptId) {
        return adminPromptService.getPrompt(promptId);
    }

    @Override
    public void deletePrompt(Long promptId, Long adminId) {
        adminPromptService.deletePrompt(promptId, adminId);
    }

    @Override
    public AdminPromptResponseDto togglePromptVisibility(Long promptId, Long adminId, PromptVisibilityRequestDto requestDto) {
        return adminPromptService.togglePromptVisibility(promptId, adminId, requestDto);
    }

    // ======================
    //      신고 처리
    // ======================

    @Override
    public Page<ReportResponseDto> getReports(ReportStatus status, Pageable pageable) {
        return adminReportService.getReports(status, pageable);
    }

    @Override
    public ReportDetailResponseDto getReportDetail(Long reportId) {
        return adminReportService.getReportDetail(reportId);
    }

    @Override
    public ReportDetailResponseDto processReport(Long reportId, Long adminId, ReportProcessRequestDto requestDto) {
        return adminReportService.processReport(reportId, adminId, requestDto);
    }

    // ======================
    //      감사 로그 조회
    // ======================

    @Override
    public Page<AuditLogResponseDto> getAuditLogs(
            Long actorId,
            AuditEntityType entityType,
            AuditAction action,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    ) {
        return adminAuditService.getAuditLogs(actorId, entityType, action, startDate, endDate, pageable);
    }

    // ======================
    //      팔로우 관리
    // ======================

    @Override
    public Page<UserResponseDto> getFollows(
            Long followerId,
            Long followingId,
            FollowStatus status,
            Pageable pageable
    ) {
        return adminFollowService.getFollows(followerId, followingId, status, pageable);
    }
}

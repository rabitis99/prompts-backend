package org.example.sharedprompts.domain.admin.service;

import org.example.sharedprompts.dto.admin.request.AuditLogFilterRequestDto;
import org.example.sharedprompts.dto.admin.request.AuthAuditLogFilterRequestDto;
import org.example.sharedprompts.dto.admin.request.PromptVisibilityRequestDto;
import org.example.sharedprompts.dto.admin.request.RateLimitLogFilterRequestDto;
import org.example.sharedprompts.dto.admin.request.UserBlockRequestDto;
import org.example.sharedprompts.dto.admin.request.UserRoleChangeRequestDto;
import org.example.sharedprompts.dto.admin.response.AdminPromptResponseDto;
import org.example.sharedprompts.dto.admin.response.AdminUserResponseDto;
import org.example.sharedprompts.dto.admin.response.RateLimitLogResponseDto;
import org.example.sharedprompts.dto.admin.response.RateLimitLogStatisticsResponseDto;
import org.example.sharedprompts.dto.audit.response.AuditLogResponseDto;
import org.example.sharedprompts.dto.audit.response.AuthAuditLogResponseDto;
import org.example.sharedprompts.domain.audit.auth.enums.AuthEventType;
import org.example.sharedprompts.domain.audit.auth.enums.AuthFailReason;
import org.example.sharedprompts.domain.audit.enums.AuditAction;
import org.example.sharedprompts.domain.audit.enums.AuditEntityType;
import org.example.sharedprompts.domain.rate.ratelimitlog.enums.RateLimitType;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.example.sharedprompts.domain.follow.FollowStatus;
import org.example.sharedprompts.domain.report.enums.ReportStatus;
import org.example.sharedprompts.dto.user.response.UserResponseDto;
import org.example.sharedprompts.dto.report.request.ReportProcessRequestDto;
import org.example.sharedprompts.dto.report.response.ReportDetailResponseDto;
import org.example.sharedprompts.dto.report.response.ReportResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface AdminService {

    // ======================
    //      사용자 관리
    // ======================

    /**
     * 사용자 목록 조회
     */
    Page<AdminUserResponseDto> getUsers(Pageable pageable);

    /**
     * 사용자 검색 (닉네임, 이메일)
     */
    Page<AdminUserResponseDto> searchUsers(String keyword, Pageable pageable);

    /**
     * 사용자 상세 조회
     */
    AdminUserResponseDto getUser(Long userId);

    /**
     * 사용자 차단/해제
     */
    AdminUserResponseDto blockUser(Long userId, Long adminId, UserBlockRequestDto requestDto);

    /**
     * 사용자 권한 변경
     */
    AdminUserResponseDto changeUserRole(Long userId, Long adminId, UserRoleChangeRequestDto requestDto);

    // ======================
    //      프롬프트 관리
    // ======================

    /**
     * 프롬프트 목록 조회
     */
    Page<AdminPromptResponseDto> getPrompts(Pageable pageable);

    /**
     * 프롬프트 검색 (제목, 작성자 닉네임)
     */
    Page<AdminPromptResponseDto> searchPrompts(String keyword, Pageable pageable);

    /**
     * 프롬프트 상세 조회
     */
    AdminPromptResponseDto getPrompt(Long promptId);

    /**
     * 프롬프트 삭제
     */
    void deletePrompt(Long promptId, Long adminId);

    /**
     * 프롬프트 공개/비공개 전환
     */
    AdminPromptResponseDto togglePromptVisibility(Long promptId, Long adminId, PromptVisibilityRequestDto requestDto);

    // ======================
    //      신고 처리
    // ======================

    /**
     * 신고 목록 조회
     */
    Page<ReportResponseDto> getReports(ReportStatus status, Pageable pageable);

    /**
     * 신고 상세 조회
     */
    ReportDetailResponseDto getReportDetail(Long reportId);

    /**
     * 신고 처리
     */
    ReportDetailResponseDto processReport(Long reportId, Long adminId, ReportProcessRequestDto requestDto);

    // ======================
    //      감사 로그 조회
    // ======================

    /**
     * 관리자 활동 감사 로그 조회
     */
    Page<AuditLogResponseDto> getAuditLogs(
            Long actorId,
            AuditEntityType entityType,
            AuditAction action,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * 관리자 활동 감사 로그 조회 - 조건 DTO로 한번에 전달
     */
    default Page<AuditLogResponseDto> getAuditLogs(
            AuditLogFilterRequestDto condition,
            Pageable pageable
    ) {
        return getAuditLogs(
                condition.getActorId(),
                condition.getEntityType(),
                condition.getAction(),
                condition.getStartDate(),
                condition.getEndDate(),
                pageable
        );
    }

    /**
     * 인증 보안 이벤트 로그 조회
     */
    Page<AuthAuditLogResponseDto> getAuthAuditLogs(
            AuthEventType eventType,
            Provider provider,
            Long userId,
            AuthFailReason failReason,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * 인증 보안 이벤트 로그 조회 - 조건 DTO로 한번에 전달
     */
    default Page<AuthAuditLogResponseDto> getAuthAuditLogs(
            AuthAuditLogFilterRequestDto condition,
            Pageable pageable
    ) {
        return getAuthAuditLogs(
            condition.getEventType(),
            condition.getProvider(),
            condition.getUserId(),
            condition.getFailReason(),
            condition.getStartDate(),
            condition.getEndDate(),
            pageable
        );
    }

    // ======================
    //      팔로우 관리
    // ======================

    /**
     * 팔로우 관계 조회 (관리자용)
     *
     * - followerId, followingId는 선택적으로 전달 가능
     * - status는 필수 (상태별 필터링)
     */
    Page<UserResponseDto> getFollows(
            Long followerId,
            Long followingId,
            FollowStatus status,
            Pageable pageable
    );

    // ======================
    //      Rate Limit 로그 조회
    // ======================

    /**
     * Rate Limit 로그 조회
     */
    Page<RateLimitLogResponseDto> getRateLimitLogs(
            Long userId,
            String clientIp,
            String ruleName,
            RateLimitType rateLimitType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * Rate Limit 로그 조회 - 조건 DTO로 한번에 전달
     */
    default Page<RateLimitLogResponseDto> getRateLimitLogs(
            RateLimitLogFilterRequestDto condition,
            Pageable pageable
    ) {
        return getRateLimitLogs(
                condition.getUserId(),
                condition.getClientIp(),
                condition.getRuleName(),
                condition.getRateLimitType(),
                condition.getStartDate(),
                condition.getEndDate(),
                pageable
        );
    }

    /**
     * Rate Limit 로그 통계 조회
     */
    RateLimitLogStatisticsResponseDto getRateLimitLogStatistics(
            LocalDateTime startDate,
            LocalDateTime endDate
    );

}


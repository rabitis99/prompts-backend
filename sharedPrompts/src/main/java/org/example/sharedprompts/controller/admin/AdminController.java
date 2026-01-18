package org.example.sharedprompts.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.admin.service.AdminService;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.domain.report.enums.ReportStatus;
import org.example.sharedprompts.dto.admin.request.PromptVisibilityRequestDto;
import org.example.sharedprompts.dto.admin.request.UserBlockRequestDto;
import org.example.sharedprompts.dto.admin.request.UserRoleChangeRequestDto;
import org.example.sharedprompts.domain.audit.enums.AuditAction;
import org.example.sharedprompts.domain.audit.enums.AuditEntityType;
import org.example.sharedprompts.dto.admin.response.AdminPromptResponseDto;
import org.example.sharedprompts.dto.admin.response.AdminUserResponseDto;
import org.example.sharedprompts.dto.audit.response.AuditLogResponseDto;
import org.example.sharedprompts.dto.report.request.ReportProcessRequestDto;
import org.example.sharedprompts.dto.report.response.ReportDetailResponseDto;
import org.example.sharedprompts.dto.report.response.ReportResponseDto;
import org.example.sharedprompts.global.annotation.AdminOnly;
import org.example.sharedprompts.global.response.CustomResponse;
import org.example.sharedprompts.global.response.CustomResponseHelper;
import org.example.sharedprompts.global.response.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@AdminOnly
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ======================
    //      사용자 관리
    // ======================

    /**
     * 사용자 목록 조회
     */
    @GetMapping("/users")
    public ResponseEntity<CustomResponse<PageResponse<AdminUserResponseDto>>> getUsers(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<AdminUserResponseDto> page = (keyword != null && !keyword.trim().isEmpty())
                ? adminService.searchUsers(keyword, pageable)
                : adminService.getUsers(pageable);
        return CustomResponseHelper.ok(PageResponse.of(page));
    }

    /**
     * 사용자 상세 조회
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<CustomResponse<AdminUserResponseDto>> getUser(@PathVariable Long id) {
        AdminUserResponseDto response = adminService.getUser(id);
        return CustomResponseHelper.ok(response);
    }

    /**
     * 사용자 차단/해제
     */
    @PatchMapping("/users/{id}/block")
    public ResponseEntity<CustomResponse<AdminUserResponseDto>> blockUser(
            @PathVariable Long id,
            @CurrentUser AuthUser authUser,
            @Valid @RequestBody UserBlockRequestDto requestDto
    ) {
        AdminUserResponseDto response = adminService.blockUser(id, authUser.getId(), requestDto);
        return CustomResponseHelper.ok(response);
    }

    /**
     * 사용자 권한 변경
     */
    @PatchMapping("/users/{id}/role")
    public ResponseEntity<CustomResponse<AdminUserResponseDto>> changeUserRole(
            @PathVariable Long id,
            @CurrentUser AuthUser authUser,
            @Valid @RequestBody UserRoleChangeRequestDto requestDto
    ) {
        AdminUserResponseDto response = adminService.changeUserRole(id, authUser.getId(), requestDto);
        return CustomResponseHelper.ok(response);
    }

    // ======================
    //      프롬프트 관리
    // ======================

    /**
     * 프롬프트 목록 조회
     */
    @GetMapping("/prompts")
    public ResponseEntity<CustomResponse<PageResponse<AdminPromptResponseDto>>> getPrompts(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<AdminPromptResponseDto> page = (keyword != null && !keyword.trim().isEmpty())
                ? adminService.searchPrompts(keyword, pageable)
                : adminService.getPrompts(pageable);
        return CustomResponseHelper.ok(PageResponse.of(page));
    }

    /**
     * 프롬프트 상세 조회
     */
    @GetMapping("/prompts/{id}")
    public ResponseEntity<CustomResponse<AdminPromptResponseDto>> getPrompt(@PathVariable Long id) {
        AdminPromptResponseDto response = adminService.getPrompt(id);
        return CustomResponseHelper.ok(response);
    }

    /**
     * 프롬프트 삭제
     */
    @DeleteMapping("/prompts/{id}")
    public ResponseEntity<Void> deletePrompt(
            @PathVariable Long id,
            @CurrentUser AuthUser authUser
    ) {
        adminService.deletePrompt(id, authUser.getId());
        return CustomResponseHelper.noContent();
    }

    /**
     * 프롬프트 공개/비공개 전환
     */
    @PatchMapping("/prompts/{id}/visibility")
    public ResponseEntity<CustomResponse<AdminPromptResponseDto>> togglePromptVisibility(
            @PathVariable Long id,
            @CurrentUser AuthUser authUser,
            @Valid @RequestBody PromptVisibilityRequestDto requestDto
    ) {
        AdminPromptResponseDto response = adminService.togglePromptVisibility(id, authUser.getId(), requestDto);
        return CustomResponseHelper.ok(response);
    }

    // ======================
    //      신고 처리
    // ======================

    /**
     * 신고 목록 조회
     */
    @GetMapping("/reports")
    public ResponseEntity<CustomResponse<PageResponse<ReportResponseDto>>> getReports(
            @RequestParam(required = false) ReportStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<ReportResponseDto> page = adminService.getReports(status, pageable);
        return CustomResponseHelper.ok(PageResponse.of(page));
    }

    /**
     * 신고 상세 조회
     */
    @GetMapping("/reports/{id}")
    public ResponseEntity<CustomResponse<ReportDetailResponseDto>> getReportDetail(@PathVariable Long id) {
        ReportDetailResponseDto response = adminService.getReportDetail(id);
        return CustomResponseHelper.ok(response);
    }

    /**
     * 신고 처리
     */
    @PatchMapping("/reports/{id}/process")
    public ResponseEntity<CustomResponse<ReportDetailResponseDto>> processReport(
            @PathVariable Long id,
            @CurrentUser AuthUser authUser,
            @Valid @RequestBody ReportProcessRequestDto requestDto
    ) {
        ReportDetailResponseDto response = adminService.processReport(id, authUser.getId(), requestDto);
        return CustomResponseHelper.ok(response);
    }

    // ======================
    //      감사 로그 조회
    // ======================

    /**
     * 감사 로그 조회 (필터링 가능)
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<CustomResponse<PageResponse<AuditLogResponseDto>>> getAuditLogs(
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) AuditEntityType entityType,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(size = 50) Pageable pageable
    ) {
        Page<AuditLogResponseDto> page = adminService.getAuditLogs(actorId, entityType, action, startDate, endDate, pageable);
        return CustomResponseHelper.ok(PageResponse.of(page));
    }
}


package org.example.sharedprompts.domain.admin.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.admin.util.AdminAuditLogger;
import org.example.sharedprompts.domain.admin.util.AdminEntityFinder;
import org.example.sharedprompts.domain.admin.validator.AdminValidator;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.enums.Role;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.dto.admin.request.UserBlockRequestDto;
import org.example.sharedprompts.dto.admin.request.UserRoleChangeRequestDto;
import org.example.sharedprompts.dto.admin.response.AdminUserResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.domain.user.service.UserSecurityEvents;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final AdminValidator adminValidator;
    private final AdminEntityFinder entityFinder;
    private final AdminAuditLogger adminAuditLogger;
    private final UserSecurityEvents userSecurityEvents;

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
            userSecurityEvents.onAccountBlocked(userId);
        } else {
            user.unblock();
            log.info("사용자 차단 해제: userId={}, adminId={}", userId, adminId);
            userSecurityEvents.onAccountUnblocked(userId);
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

        userSecurityEvents.onRoleChanged(userId);

        User admin = entityFinder.findUserById(adminId);
        adminAuditLogger.logUserRoleChange(admin, userId, oldRole.name(), requestDto.getRole().name());

        return AdminUserResponseDto.from(user);
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? null : keyword.trim();
    }
}


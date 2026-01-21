package org.example.sharedprompts.domain.admin.service;

import org.example.sharedprompts.dto.admin.request.UserBlockRequestDto;
import org.example.sharedprompts.dto.admin.request.UserRoleChangeRequestDto;
import org.example.sharedprompts.dto.admin.response.AdminUserResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {

    Page<AdminUserResponseDto> getUsers(Pageable pageable);

    Page<AdminUserResponseDto> searchUsers(String keyword, Pageable pageable);

    AdminUserResponseDto getUser(Long userId);

    AdminUserResponseDto blockUser(Long userId, Long adminId, UserBlockRequestDto requestDto);

    AdminUserResponseDto changeUserRole(Long userId, Long adminId, UserRoleChangeRequestDto requestDto);
}



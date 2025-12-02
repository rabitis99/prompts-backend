package org.example.sharedprompts.domain.user.service;

import org.example.sharedprompts.dto.user.request.PasswordChangeRequestDto;
import org.example.sharedprompts.dto.user.request.UserUpdateRequestDto;
import org.example.sharedprompts.dto.user.response.UserResponseDto;

public interface UserService {

    UserResponseDto getMyInfo(Long userId);

    UserResponseDto updateMyInfo(Long userId, UserUpdateRequestDto requestDto);

    UserResponseDto changePassword(Long userId, PasswordChangeRequestDto requestDto);

    void deleteUser(Long targetUserId, Long requesterId);

}

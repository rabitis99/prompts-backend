package org.example.sharedprompts.domain.user.service;

import org.example.sharedprompts.dto.user.request.PasswordChangeRequestDto;
import org.example.sharedprompts.dto.user.request.UserUpdateRequestDto;
import org.example.sharedprompts.dto.user.response.UserPublicProfileDto;
import org.example.sharedprompts.dto.user.response.UserResponseDto;

public interface UserService {

    UserResponseDto getMyInfo(Long userId);

    UserResponseDto updateMyInfo(Long userId, UserUpdateRequestDto requestDto);

    UserResponseDto changePassword(Long userId, PasswordChangeRequestDto requestDto);

    void deleteUser(Long targetUserId, Long requesterId);

    /**
     * 공개 프로필 정보를 조회합니다.
     * 팔로워/팔로잉 수 및 공개 여부를 포함합니다.
     *
     * @param targetUserId 조회 대상 사용자 ID
     * @param viewerId 조회하는 사용자 ID (null일 경우 비로그인 사용자)
     * @return UserPublicProfileDto
     */
    UserPublicProfileDto getPublicProfile(Long targetUserId, Long viewerId);

}

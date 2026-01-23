package org.example.sharedprompts.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.follow.FollowStatus;
import org.example.sharedprompts.domain.follow.PublicFollowState;
import org.example.sharedprompts.domain.follow.service.FollowService;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.example.sharedprompts.domain.user.enums.Role;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.domain.user.validator.PasswordStrengthValidator;
import org.example.sharedprompts.dto.follow.response.FollowCountResponseDto;
import org.example.sharedprompts.dto.user.request.PasswordChangeRequestDto;
import org.example.sharedprompts.dto.user.request.UserUpdateRequestDto;
import org.example.sharedprompts.dto.user.response.UserPublicProfileDto;
import org.example.sharedprompts.dto.user.response.UserResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.util.HtmlSanitizer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordStrengthValidator passwordStrengthValidator;
    private final UserSecurityEvents userSecurityEvents;
    private final FollowService followService;
    private final HtmlSanitizer htmlSanitizer;

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getMyInfo(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        return UserResponseDto.from(user);
    }

    @Override
    @Transactional
    public UserResponseDto updateMyInfo(Long userId, UserUpdateRequestDto requestDto) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        // 프로필 입력값 Sanitization
        requestDto.sanitize(htmlSanitizer);

        requestDto.applyTo(user);

        return UserResponseDto.from(user);
    }

    @Override
    @Transactional
    public UserResponseDto changePassword(Long userId, PasswordChangeRequestDto requestDto) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        if (user.getProvider() != Provider.LOCAL) {
            throw new ApiException(ErrorCode.NOT_LOCAL_USER);
        }

        if (!passwordEncoder.matches(requestDto.getCurrentPassword(), user.getPassword())) {
            throw new ApiException(ErrorCode.INVALID_PASSWORD);
        }

        if (requestDto.getCurrentPassword().equals(requestDto.getNewPassword())) {
            throw new ApiException(ErrorCode.SAME_AS_CURRENT_PASSWORD);
        }

        // 비밀번호 강도 검증
        passwordStrengthValidator.validate(requestDto.getNewPassword());

        String encodedPassword = passwordEncoder.encode(requestDto.getNewPassword());
        user.changePassword(encodedPassword);

        // 비밀번호 변경 시 토큰 무효화
        userSecurityEvents.onPasswordChanged(userId);

        return UserResponseDto.from(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long targetUserId, Long requesterId) {
        User targetUser = userRepository.findByIdAndDeletedAtIsNull(targetUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        User requester = targetUserId.equals(requesterId)
                ? targetUser
                : userRepository.findByIdAndDeletedAtIsNull(requesterId)
                    .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        // 관리자 또는 본인만 삭제 가능
        boolean isAdmin = requester.getRole() == Role.ROLE_ADMIN;
        boolean isSelf = targetUserId.equals(requesterId);

        if (!isAdmin && !isSelf) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }

        // Soft delete 수행
        // findByIdAndDeletedAtIsNull로 이미 삭제되지 않은 사용자만 조회되므로 추가 검증 불필요
        targetUser.softDelete();
        userSecurityEvents.onAccountDeleted(targetUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserPublicProfileDto getPublicProfile(Long targetUserId, Long viewerId) {
        // 대상 사용자 조회
        User targetUser = userRepository.findByIdAndDeletedAtIsNull(targetUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        // 팔로워/팔로잉 수 조회 (FOLLOWING 상태만 카운트)
        FollowCountResponseDto followCount = followService.getFollowCount(
                targetUserId,
                FollowStatus.FOLLOWING
        );

        // 팔로우 상태 조회 (viewer → target 방향)
        PublicFollowState publicFollowState = followService.getPublicFollowState(viewerId, targetUserId);

        return UserPublicProfileDto.from(
                targetUser,
                followCount.getFollowersCount(),
                followCount.getFollowingCount(),
                publicFollowState
        );
    }
}


package org.example.sharedprompts.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.example.sharedprompts.domain.user.enums.Role;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.dto.user.request.PasswordChangeRequestDto;
import org.example.sharedprompts.dto.user.request.UserUpdateRequestDto;
import org.example.sharedprompts.dto.user.response.UserResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getMyInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        return UserResponseDto.from(user);
    }

    @Override
    public UserResponseDto updateMyInfo(Long userId, UserUpdateRequestDto requestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        User updatedUser = requestDto.applyTo(user);

        return UserResponseDto.from(updatedUser);
    }

    @Override
    public UserResponseDto changePassword(Long userId, PasswordChangeRequestDto requestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        if (user.getProvider() != Provider.LOCAL) {
            throw new ApiException(ErrorCode.NOT_LOCAL_USER);
        }

        if (!passwordEncoder.matches(requestDto.getCurrentPassword(), user.getPassword())) {
            throw new ApiException(ErrorCode.INVALID_PASSWORD);
        }

        String encodedPassword = passwordEncoder.encode(requestDto.getNewPassword());
        user.changePassword(encodedPassword);

        return UserResponseDto.from(user);
    }

    @Override
    public void deleteUser(Long targetUserId, Long requesterId) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        // 관리자 또는 본인만 삭제 가능
        boolean isAdmin = requester.getRole() == Role.ROLE_ADMIN;
        boolean isSelf = targetUserId.equals(requesterId);

        if (!isAdmin && !isSelf) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }

        userRepository.delete(targetUser);
    }
}


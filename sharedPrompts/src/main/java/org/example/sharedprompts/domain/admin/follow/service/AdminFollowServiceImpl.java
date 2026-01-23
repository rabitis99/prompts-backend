package org.example.sharedprompts.domain.admin.follow.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.admin.validator.AdminValidator;
import org.example.sharedprompts.domain.follow.FollowStatus;
import org.example.sharedprompts.domain.follow.repository.admin.AdminFollowRepository;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.dto.user.response.UserResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminFollowServiceImpl implements AdminFollowService {

    private final AdminFollowRepository adminFollowRepository;
    private final AdminValidator adminValidator;

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDto> getFollows(
            Long followerId,
            Long followingId,
            FollowStatus status,
            Pageable pageable
    ) {
        adminValidator.validatePageSize(pageable, 100);
        // AdminFollowRepository 계약: status는 필수 (null 전달 시 전체 스캔 방지)
        if (status == null) {
            throw new ApiException(ErrorCode.FOLLOW_STATUS_REQUIRED);
        }

        Page<User> page = adminFollowRepository.findUsersByCondition(
                followerId,
                followingId,
                status,
                pageable
        );

        return page.map(UserResponseDto::from);
    }
}


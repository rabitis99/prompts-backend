package org.example.sharedprompts.domain.admin.service;

import org.example.sharedprompts.domain.follow.FollowStatus;
import org.example.sharedprompts.dto.user.response.UserResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminFollowService {

    Page<UserResponseDto> getFollows(
            Long followerId,
            Long followingId,
            FollowStatus status,
            Pageable pageable
    );
}



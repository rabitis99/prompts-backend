package org.example.sharedprompts.domain.follow.service;

import org.example.sharedprompts.domain.follow.FollowStatus;
import org.example.sharedprompts.dto.follow.response.FollowCountResponseDto;
import org.example.sharedprompts.dto.follow.response.FollowResponseDto;
import org.example.sharedprompts.dto.follow.response.FollowUserResponseDto;
import org.example.sharedprompts.global.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface FollowService {
    void requestFollow(Long followerId, Long followingId);
    void acceptFollow(Long followerId, Long followingId);
    void rejectFollow(Long followerId, Long followingId);
    void blockFollow(Long followerId, Long followingId);
    void unblockFollow(Long followerId, Long followingId);
    void unfollow(Long followerId, Long followingId);
    FollowResponseDto getFollowStatus(Long followerId, Long followingId);
    PageResponse<FollowUserResponseDto> getFollowers(Long userId, Long viewerId, FollowStatus status, Pageable pageable);
    PageResponse<FollowUserResponseDto> getFollowing(Long userId, Long viewerId, FollowStatus status, Pageable pageable);
    FollowCountResponseDto getFollowCount(Long userId, FollowStatus status);
}


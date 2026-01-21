package org.example.sharedprompts.domain.follow.service;

import org.example.sharedprompts.domain.follow.FollowStatus;
import org.example.sharedprompts.domain.follow.PublicFollowState;
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
    void removeFollower(Long meId, Long followerId);
    FollowResponseDto getFollowStatus(Long followerId, Long followingId);
    /**
     * viewer → target 방향의 PublicFollowState를 반환합니다.
     * 공개 프로필 조회 시 사용됩니다.
     *
     * @param viewerId 조회하는 사용자 ID (null 가능)
     * @param targetUserId 조회 대상 사용자 ID
     * @return PublicFollowState
     */
    PublicFollowState getPublicFollowState(Long viewerId, Long targetUserId);
    PageResponse<FollowUserResponseDto> getFollowers(Long userId, Long viewerId, FollowStatus status, Pageable pageable);
    PageResponse<FollowUserResponseDto> getFollowing(Long userId, Long viewerId, FollowStatus status, Pageable pageable);
    FollowCountResponseDto getFollowCount(Long userId, FollowStatus status);
}


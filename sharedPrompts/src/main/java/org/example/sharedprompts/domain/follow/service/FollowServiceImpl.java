package org.example.sharedprompts.domain.follow.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.follow.Follow;
import org.example.sharedprompts.domain.follow.FollowStatus;
import org.example.sharedprompts.domain.follow.repository.FollowRepository;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.dto.follow.response.FollowCountResponseDto;
import org.example.sharedprompts.dto.follow.response.FollowResponseDto;
import org.example.sharedprompts.dto.user.response.UserResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.response.PageResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void requestFollow(Long followerId, Long followingId) {
        validateIds(followerId, followingId);
        validateUserExists(followerId);
        validateUserExists(followingId);
        validateNotSelfFollow(followerId, followingId);

        if (followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new ApiException(ErrorCode.FOLLOW_ALREADY_EXISTS);
        }

        Follow follow = new Follow(followerId, followingId);
        try {
            followRepository.save(follow);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.FOLLOW_ALREADY_EXISTS);
        }
    }

    @Override
    @Transactional
    public void acceptFollow(Long followerId, Long followingId) {
        validateIds(followerId, followingId);
        Follow follow = findFollow(followerId, followingId);
        
        if (follow.getStatus() != FollowStatus.PENDING) {
            throw new ApiException(ErrorCode.FOLLOW_NOT_PENDING);
        }

        follow.markFollowing();
        followRepository.save(follow);
    }

    @Override
    @Transactional
    public void rejectFollow(Long followerId, Long followingId) {
        validateIds(followerId, followingId);
        Follow follow = findFollow(followerId, followingId);
        
        if (follow.getStatus() != FollowStatus.PENDING) {
            throw new ApiException(ErrorCode.FOLLOW_NOT_PENDING);
        }

        followRepository.delete(follow);
    }

    @Override
    @Transactional
    public void blockFollow(Long followerId, Long followingId) {
        validateIds(followerId, followingId);
        validateUserExists(followerId);
        validateUserExists(followingId);
        validateNotSelfFollow(followerId, followingId);

        Follow follow = followRepository
                .findByFollowerIdAndFollowingId(followerId, followingId)
                .orElseGet(() -> new Follow(followerId, followingId));

        follow.markBlocked();
        followRepository.save(follow);
    }

    @Override
    @Transactional
    public void unblockFollow(Long followerId, Long followingId) {
        validateIds(followerId, followingId);
        Follow follow = findFollow(followerId, followingId);
        
        if (follow.getStatus() != FollowStatus.BLOCKED) {
            throw new ApiException(ErrorCode.FOLLOW_NOT_BLOCKED);
        }

        followRepository.delete(follow);
    }

    @Override
    @Transactional
    public void unfollow(Long followerId, Long followingId) {
        validateIds(followerId, followingId);
        Follow follow = findFollow(followerId, followingId);
        followRepository.delete(follow);
    }

    @Override
    @Transactional(readOnly = true)
    public FollowResponseDto getFollowStatus(Long followerId, Long followingId) {
        validateIds(followerId, followingId);
        return followRepository
                .findByFollowerIdAndFollowingId(followerId, followingId)
                .map(follow -> FollowResponseDto.from(follow.getStatus()))
                .orElse(FollowResponseDto.from(null));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponseDto> getFollowers(Long userId, FollowStatus status, Pageable pageable) {
        Page<User> page = followRepository.findFollowersByUserIdAndStatus(userId, status, pageable);
        return PageResponse.of(page.map(UserResponseDto::from));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponseDto> getFollowing(Long userId, FollowStatus status, Pageable pageable) {
        Page<User> page = followRepository.findFollowingByUserIdAndStatus(userId, status, pageable);
        return PageResponse.of(page.map(UserResponseDto::from));
    }

    @Override
    @Transactional(readOnly = true)
    public FollowCountResponseDto getFollowCount(Long userId, FollowStatus status) {
        Long followersCount = followRepository.countFollowersByUserIdAndStatus(userId, status);
        Long followingCount = followRepository.countFollowingByUserIdAndStatus(userId, status);
        
        return FollowCountResponseDto.builder()
                .followersCount(followersCount)
                .followingCount(followingCount)
                .build();
    }

    private void validateIds(Long followerId, Long followingId) {
        if (followerId == null || followingId == null) {
            throw new ApiException(ErrorCode.FOLLOW_IDS_REQUIRED);
        }
    }

    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private void validateNotSelfFollow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new ApiException(ErrorCode.CANNOT_FOLLOW_SELF);
        }
    }

    private Follow findFollow(Long followerId, Long followingId) {
        return followRepository
                .findByFollowerIdAndFollowingId(followerId, followingId)
                .orElseThrow(() -> new ApiException(ErrorCode.FOLLOW_NOT_FOUND));
    }
}


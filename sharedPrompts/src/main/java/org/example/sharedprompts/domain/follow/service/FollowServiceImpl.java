package org.example.sharedprompts.domain.follow.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.follow.Follow;
import org.example.sharedprompts.domain.follow.FollowStatus;
import org.example.sharedprompts.domain.follow.event.FollowEvent;
import org.example.sharedprompts.domain.follow.repository.FollowRepository;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.dto.follow.response.FollowCountResponseDto;
import org.example.sharedprompts.dto.follow.response.FollowResponseDto;
import org.example.sharedprompts.dto.follow.response.FollowUserResponseDto;
import org.example.sharedprompts.dto.follow.response.UserWithFollowInfo;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.response.PageResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class FollowServiceImpl implements FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    /* ================= 팔로우 요청 ================= */

    @Override
    public void requestFollow(Long followerId, Long followingId) {
        validateRequest(followerId, followingId);

        Follow follow = followRepository
                .findByFollowerIdAndFollowingId(followerId, followingId)
                .orElse(null);

        if (follow == null) {
            follow = new Follow(followerId, followingId);
            followRepository.save(follow);
            eventPublisher.publishEvent(new FollowEvent.Requested(followerId, followingId));
            return;
        }

        FollowStatus status = follow.getStatus();

        if (status == FollowStatus.REJECTED || status == FollowStatus.CANCELLED) {
            follow.markPending();
            followRepository.save(follow);
            eventPublisher.publishEvent(new FollowEvent.Requested(followerId, followingId));
            return;
        }

        if (status == FollowStatus.BLOCKED) {
            throw new ApiException(ErrorCode.FOLLOW_BLOCKED);
        }

        throw new ApiException(ErrorCode.FOLLOW_ALREADY_EXISTS);
    }

    /* ================= 요청 수락 / 거절 ================= */

    @Override
    public void acceptFollow(Long followerId, Long followingId) {
        Follow follow = findFollow(followerId, followingId);

        if (follow.getStatus() != FollowStatus.PENDING) {
            throw new ApiException(ErrorCode.FOLLOW_NOT_PENDING);
        }

        follow.markFollowing();
        followRepository.save(follow);
    }

    @Override
    public void rejectFollow(Long followerId, Long followingId) {
        Follow follow = findFollow(followerId, followingId);

        if (follow.getStatus() != FollowStatus.PENDING) {
            throw new ApiException(ErrorCode.FOLLOW_NOT_PENDING);
        }

        follow.markRejected();
        followRepository.save(follow);
    }

    /* ================= 언팔 / 차단 ================= */

    @Override
    public void unfollow(Long followerId, Long followingId) {
        Follow follow = findFollow(followerId, followingId);

        FollowStatus status = follow.getStatus();

        if (status == FollowStatus.BLOCKED) {
            throw new ApiException(ErrorCode.FOLLOW_BLOCKED);
        }

        if (status == FollowStatus.CANCELLED || status == FollowStatus.REJECTED) {
            // 이미 종료된 관계 → no-op
            return;
        }

        follow.markCancelled();
        followRepository.save(follow);
    }

    @Override
    public void blockFollow(Long followerId, Long followingId) {
        validateRequest(followerId, followingId);

        Follow follow = followRepository
                .findByFollowerIdAndFollowingId(followerId, followingId)
                .orElseGet(() -> new Follow(followerId, followingId));

        follow.markBlocked();
        followRepository.save(follow);
    }

    @Override
    public void unblockFollow(Long followerId, Long followingId) {
        Follow follow = findFollow(followerId, followingId);

        if (follow.getStatus() != FollowStatus.BLOCKED) {
            throw new ApiException(ErrorCode.FOLLOW_NOT_BLOCKED);
        }

        follow.markPending();
        followRepository.save(follow);
    }

    /* ================= 조회 ================= */

    @Override
    @Transactional(readOnly = true)
    public FollowResponseDto getFollowStatus(Long followerId, Long followingId) {
        validateIds(followerId, followingId);

        // viewer → target 방향 Follow 조회
        Optional<Follow> forwardFollow = followRepository
                .findByFollowerIdAndFollowingId(followerId, followingId);
        
        // target → viewer 방향 Follow 조회 (양방향 정보)
        Optional<Follow> reverseFollow = followRepository
                .findByFollowerIdAndFollowingId(followingId, followerId);

        return FollowResponseDto.from(
                forwardFollow.orElse(null),
                reverseFollow.orElse(null)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FollowUserResponseDto> getFollowers(
            Long userId, Long viewerId, FollowStatus status, Pageable pageable) {

        Page<UserWithFollowInfo> page =
                followRepository.findFollowersWithFollowInfo(userId, viewerId, status, pageable);

        return PageResponse.of(page.map(this::toFollowUserResponseDto));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FollowUserResponseDto> getFollowing(
            Long userId, Long viewerId, FollowStatus status, Pageable pageable) {

        Page<UserWithFollowInfo> page =
                followRepository.findFollowingWithFollowInfo(userId, viewerId, status, pageable);

        return PageResponse.of(page.map(this::toFollowUserResponseDto));
    }

    @Override
    @Transactional(readOnly = true)
    public FollowCountResponseDto getFollowCount(Long userId, FollowStatus status) {
        return FollowCountResponseDto.builder()
                .followersCount(
                        followRepository.countFollowersByUserIdAndStatus(userId, status))
                .followingCount(
                        followRepository.countFollowingByUserIdAndStatus(userId, status))
                .build();
    }

    /* ================= 검증 / 헬퍼 ================= */

    private void validateRequest(Long followerId, Long followingId) {
        validateIds(followerId, followingId);

        if (followerId.equals(followingId)) {
            throw new ApiException(ErrorCode.CANNOT_FOLLOW_SELF);
        }

        if (!userRepository.existsById(followerId)
                || !userRepository.existsById(followingId)) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private void validateIds(Long followerId, Long followingId) {
        if (followerId == null || followingId == null) {
            throw new ApiException(ErrorCode.FOLLOW_IDS_REQUIRED);
        }
    }

    private Follow findFollow(Long followerId, Long followingId) {
        return followRepository
                .findByFollowerIdAndFollowingId(followerId, followingId)
                .orElseThrow(() -> new ApiException(ErrorCode.FOLLOW_NOT_FOUND));
    }

    /**
     * UserWithFollowInfo를 FollowUserResponseDto로 변환
     */
    private FollowUserResponseDto toFollowUserResponseDto(UserWithFollowInfo info) {
        Follow forwardFollow = info.getForwardFollow();
        Follow reverseFollow = info.getReverseFollow();

        // 상태 추출
        FollowStatus followStatus = forwardFollow != null ? forwardFollow.getStatus() : null;
        FollowStatus reverseFollowStatus = reverseFollow != null ? reverseFollow.getStatus() : null;

        // 블록 상태 확인
        Boolean isBlockedByMe = followStatus != null && followStatus == FollowStatus.BLOCKED;
        Boolean isBlockedByTarget = reverseFollowStatus != null && reverseFollowStatus == FollowStatus.BLOCKED;

        // 팔로우/요청 일시 추출
        LocalDateTime followedAt = null;
        LocalDateTime requestedAt = null;
        if (forwardFollow != null) {
            if (forwardFollow.getStatus() == FollowStatus.FOLLOWING) {
                followedAt = forwardFollow.getCreatedAt();
            } else if (forwardFollow.getStatus() == FollowStatus.PENDING) {
                requestedAt = forwardFollow.getCreatedAt();
            }
        }
        if (reverseFollow != null && reverseFollow.getStatus() == FollowStatus.PENDING) {
            requestedAt = reverseFollow.getCreatedAt();
        }

        return FollowUserResponseDto.from(
                info.getUser(),
                followStatus,
                reverseFollowStatus,
                isBlockedByMe,
                isBlockedByTarget,
                followedAt,
                requestedAt
        );
    }
}


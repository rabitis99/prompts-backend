package org.example.sharedprompts.domain.follow.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.follow.Follow;
import org.example.sharedprompts.domain.follow.FollowStatus;
import org.example.sharedprompts.domain.follow.PublicFollowState;
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
        validateBlock(followerId, followingId);

        // 팔로워 차단 = "상대 → 나" 방향을 BLOCKED로 만듦
        // 따라서 (followingId → followerId) 방향의 Follow를 찾아야 함
        Follow reverseFollow = followRepository
                .findByFollowerIdAndFollowingId(followingId, followerId)
                .orElse(null);

        // ① 상대 → 나 방향: BLOCKED 처리 (idempotent)
        if (reverseFollow == null) {
            reverseFollow = new Follow(followingId, followerId);
            reverseFollow.markBlocked();
            followRepository.save(reverseFollow);
        } else if (reverseFollow.getStatus() != FollowStatus.BLOCKED) {
            reverseFollow.markBlocked();
            followRepository.save(reverseFollow);
        }

        // ② 나 → 상대 방향: CANCELLED 처리 (양방향 FOLLOWING 상태 방지)
        Optional<Follow> forwardFollow = followRepository
                .findByFollowerIdAndFollowingId(followerId, followingId);

        if (forwardFollow.isPresent()) {
            Follow follow = forwardFollow.get();
            FollowStatus status = follow.getStatus();
            if (status == FollowStatus.FOLLOWING || status == FollowStatus.PENDING) {
                follow.markCancelled();
                followRepository.save(follow);
            }
            // REJECTED, CANCELLED는 유지, 없으면 무시
        }
    }

    @Override
    public void unblockFollow(Long followerId, Long followingId) {
        // 차단 해제도 "상대 → 나" 방향을 처리해야 함
        Follow follow = followRepository
                .findByFollowerIdAndFollowingId(followingId, followerId)
                .orElseThrow(() -> new ApiException(ErrorCode.FOLLOW_NOT_FOUND));

        if (follow.getStatus() != FollowStatus.BLOCKED) {
            throw new ApiException(ErrorCode.FOLLOW_NOT_BLOCKED);
        }

        // 차단 해제는 BLOCKED → CANCELLED만 허용 (PENDING 금지)
        follow.markCancelled();
        followRepository.save(follow);
    }

    /* ================= 팔로워 삭제 (Remove) ================= */

    /**
     * 팔로워 삭제 (Remove)
     * 
     * "상대가 나를 팔로우하고 있는 관계를 강제로 종료시키는 행위"
     * - 주체: 팔로우 당한 사람 (me)
     * - 대상: 나를 팔로우 중인 사용자 (follower)
     * 
     * 상태 전이 규칙:
     * - PENDING → CANCELLED
     * - FOLLOWING → CANCELLED
     * - REJECTED → CANCELLED
     * - CANCELLED → CANCELLED (idempotent)
     * - BLOCKED → 에러 (차단 우선)
     * - 없으면 no-op
     */
    @Override
    public void removeFollower(Long meId, Long followerId) {
        validateIds(meId, followerId);

        // (followerId → meId) 방향 Follow 조회
        Follow follow = followRepository
                .findByFollowerIdAndFollowingId(followerId, meId)
                .orElse(null);

        if (follow == null) {
            // 관계가 없으면 no-op
            return;
        }

        FollowStatus status = follow.getStatus();

        // BLOCKED 상태면 에러 (차단 우선)
        if (status == FollowStatus.BLOCKED) {
            throw new ApiException(ErrorCode.FOLLOW_BLOCKED);
        }

        // CANCELLED 상태면 idempotent (그대로 유지)
        if (status == FollowStatus.CANCELLED) {
            return;
        }

        // PENDING, FOLLOWING, REJECTED → CANCELLED
        follow.markCancelled();
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

    @Override
    @Transactional(readOnly = true)
    public PublicFollowState getPublicFollowState(Long viewerId, Long targetUserId) {
        // targetUserId가 null이면 조회 불가
        if (targetUserId == null) {
            return PublicFollowState.NONE;
        }
        
        // 비로그인 사용자 또는 본인 프로필 조회 시
        if (viewerId == null) {
            return PublicFollowState.NONE;
        }
        
        if (viewerId.equals(targetUserId)) {
            return PublicFollowState.FOLLOWING;
        }

        // viewer → target 방향 Follow 조회
        Optional<Follow> follow = followRepository
                .findByFollowerIdAndFollowingId(viewerId, targetUserId);

        FollowStatus status = follow.map(Follow::getStatus).orElse(null);
        return PublicFollowState.from(status);
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

    /**
     * 차단 전용 검증 메서드
     * - 차단은 "방어 행위"이므로 팔로우 관계 존재 여부는 검증하지 않음
     * - meId != targetId, target 사용자 존재 확인만 수행
     */
    private void validateBlock(Long meId, Long targetId) {
        validateIds(meId, targetId);

        if (meId.equals(targetId)) {
            throw new ApiException(ErrorCode.CANNOT_BLOCK_SELF);
        }

        if (!userRepository.existsById(targetId)) {
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


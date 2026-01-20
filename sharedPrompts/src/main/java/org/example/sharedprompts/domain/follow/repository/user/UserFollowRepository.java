package org.example.sharedprompts.domain.follow.repository.user;

import org.example.sharedprompts.domain.follow.Follow;
import org.example.sharedprompts.domain.follow.FollowStatus;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.dto.follow.response.UserWithFollowInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * User UX 전용 Follow 조회용 Repository.
 *
 * - 항상 followerId 또는 followingId 중 하나는 필수로 사용된다.
 * - status는 단순 필터 옵션이며 null 허용.
 * - 삭제되었거나 차단된 유저는 제외하고 조회한다.
 */
public interface UserFollowRepository {

    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    Page<User> findFollowersByUserIdAndStatus(Long userId, FollowStatus status, Pageable pageable);

    Page<User> findFollowingByUserIdAndStatus(Long userId, FollowStatus status, Pageable pageable);

    /**
     * 팔로워 목록과 양방향 Follow 정보를 함께 조회
     * @param userId 대상 사용자 ID
     * @param viewerId 조회하는 사용자 ID (양방향 관계 확인용)
     * @param status 필터링할 상태 (null이면 REJECTED, CANCELLED 제외)
     * @param pageable 페이지 정보
     * @return User와 양방향 Follow 정보를 포함한 페이지
     */
    Page<UserWithFollowInfo> findFollowersWithFollowInfo(Long userId, Long viewerId, FollowStatus status, Pageable pageable);

    /**
     * 팔로잉 목록과 양방향 Follow 정보를 함께 조회
     * @param userId 대상 사용자 ID
     * @param viewerId 조회하는 사용자 ID (양방향 관계 확인용)
     * @param status 필터링할 상태 (null이면 REJECTED, CANCELLED 제외)
     * @param pageable 페이지 정보
     * @return User와 양방향 Follow 정보를 포함한 페이지
     */
    Page<UserWithFollowInfo> findFollowingWithFollowInfo(Long userId, Long viewerId, FollowStatus status, Pageable pageable);

    /**
     * 여러 사용자에 대한 양방향 Follow 정보를 배치로 조회
     * @param viewerId 조회하는 사용자 ID
     * @param targetIds 대상 사용자 ID 목록
     * @return (targetId, forwardFollow, reverseFollow) 리스트
     */
    List<UserWithFollowInfo> findBidirectionalFollowInfo(Long viewerId, List<Long> targetIds);

    /**
     * 두 사용자 사이에 BLOCKED 상태의 Follow 관계가 존재하는지 여부
     * - 방향에 상관없이 (viewer → target, target → viewer) 모두 검사
     */
    boolean existsBlockedBetween(Long userId1, Long userId2);

    /**
     * 특정 사용자를 FOLLOWING 상태로 팔로우 중인 사용자 ID 목록 조회
     * - BLOCKED 여부는 포함하지 않으며, 순수 관계만 조회한다.
     */
    List<Long> findFollowerIdsByFollowingIdAndStatus(Long followingId, FollowStatus status);

    Long countFollowersByUserIdAndStatus(Long userId, FollowStatus status);

    Long countFollowingByUserIdAndStatus(Long userId, FollowStatus status);
}



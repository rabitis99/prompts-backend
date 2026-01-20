package org.example.sharedprompts.domain.follow.policy;

/**
 * 팔로우/차단(BLOCKED) 정책을 중앙에서 관리하는 컴포넌트.
 *
 * - userA 와 userB 사이에 BLOCKED 관계가 존재하는지 여부만 판단한다.
 * - 실제 BLOCKED 존재 여부 조회는 내부적으로 Repository를 통해 수행한다.
 */
public interface FollowBlockPolicy {

    /**
     * 두 사용자 사이에 BLOCKED 관계가 존재하는지 여부를 반환한다.
     *
     * @param userA 사용자 A ID
     * @param userB 사용자 B ID
     * @return BLOCKED 관계가 하나라도 존재하면 true
     */
    boolean isBlocked(Long userA, Long userB);
}



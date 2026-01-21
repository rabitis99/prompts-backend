package org.example.sharedprompts.domain.follow.policy;

import java.util.List;

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

    /**
     * 여러 사용자와 특정 사용자 사이에 BLOCKED 관계가 있는 사용자 ID 목록을 배치로 조회
     * - N+1 쿼리 문제를 해결하기 위한 배치 조회 메서드
     *
     * @param userIds 검사할 사용자 ID 목록
     * @param blockedById BLOCKED 관계의 기준점이 되는 사용자 ID
     * @return BLOCKED 관계가 있는 사용자 ID 목록
     */
    List<Long> findBlockedUserIds(List<Long> userIds, Long blockedById);
}



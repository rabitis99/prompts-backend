package org.example.sharedprompts.domain.notification.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.follow.FollowStatus;
import org.example.sharedprompts.domain.follow.policy.FollowBlockPolicy;
import org.example.sharedprompts.domain.follow.repository.FollowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 알림 대상 결정 기본 구현체.
 *
 * - Repository: FOLLOWING 관계만 조회
 * - Service   : BLOCKED 정책 등을 포함한 비즈니스 규칙을 조합
 */
@Service
@RequiredArgsConstructor
public class NotificationTargetServiceImpl implements NotificationTargetService {

    private final FollowRepository followRepository;
    private final FollowBlockPolicy followBlockPolicy;

    @Override
    @Transactional(readOnly = true)
    public List<Long> getPromptCreatedTargets(Long authorId) {
        // author를 FOLLOWING 상태로 팔로우 중인 사용자 ID 목록 조회 (관계 조회만)
        List<Long> followerIds = followRepository
                .findFollowerIdsByFollowingIdAndStatus(authorId, FollowStatus.FOLLOWING);

        // BLOCKED 관계가 아닌 사용자만 필터링
        return followerIds.stream()
                // 자기 자신에게는 알림 전송하지 않음 (방어적 체크)
                .filter(followerId -> !authorId.equals(followerId))
                .filter(followerId -> !followBlockPolicy.isBlocked(followerId, authorId))
                .toList();
    }
}



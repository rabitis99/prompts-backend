package org.example.sharedprompts.domain.notification.service.target;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.follow.FollowStatus;
import org.example.sharedprompts.domain.follow.policy.FollowBlockPolicy;
import org.example.sharedprompts.domain.follow.repository.FollowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 알림 대상 결정 기본 구현체.
 *
 * - Repository: FOLLOWING 관계만 조회
 * - Service   : BLOCKED 정책 등을 포함한 비즈니스 규칙을 조합
 * - 배치 조회를 통한 N+1 쿼리 문제 해결
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

        if (followerIds.isEmpty()) {
            return List.of();
        }

        // 자기 자신 제외
        List<Long> filteredFollowerIds = followerIds.stream()
                .filter(followerId -> !authorId.equals(followerId))
                .toList();

        if (filteredFollowerIds.isEmpty()) {
            return List.of();
        }

        // 배치 조회로 BLOCKED 관계가 있는 사용자 ID 목록 조회 (N+1 쿼리 방지)
        Set<Long> blockedUserIds = followBlockPolicy.findBlockedUserIds(filteredFollowerIds, authorId)
                .stream()
                .collect(Collectors.toSet());

        // BLOCKED 관계가 아닌 사용자만 필터링
        return filteredFollowerIds.stream()
                .filter(followerId -> !blockedUserIds.contains(followerId))
                .toList();
    }
}



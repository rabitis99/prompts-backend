package org.example.sharedprompts.domain.follow.policy;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.follow.repository.FollowRepository;
import org.springframework.stereotype.Component;

/**
 * FollowBlockPolicy 기본 구현체.
 *
 * - FollowRepository.existsBlockedBetween(...) 에 대한 단일 진입점.
 * - Service 계층에서는 이 컴포넌트만 사용하고, Repository 메서드를 직접 호출하지 않는다.
 */
@Component
@RequiredArgsConstructor
public class FollowBlockPolicyImpl implements FollowBlockPolicy {

    private final FollowRepository followRepository;

    @Override
    public boolean isBlocked(Long userA, Long userB) {
        if (userA == null || userB == null) {
            return false;
        }
        return followRepository.existsBlockedBetween(userA, userB);
    }
}



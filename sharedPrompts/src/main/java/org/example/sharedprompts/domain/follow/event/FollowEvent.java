package org.example.sharedprompts.domain.follow.event;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FollowEvent {

    // 팔로우 요청 이벤트
    public record Requested(Long followerId, Long followingId) {}
}


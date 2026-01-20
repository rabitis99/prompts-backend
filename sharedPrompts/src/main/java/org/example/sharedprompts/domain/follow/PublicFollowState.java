package org.example.sharedprompts.domain.follow;

/**
 * 공개 프로필용 Follow 상태 (Public API 전용).
 *
 * - 내부 FollowStatus 전체를 노출하지 않고,
 *   클라이언트에서 필요한 최소 상태만 제공한다.
 */
public enum PublicFollowState {
    FOLLOWING,
    PENDING,
    NONE;

    /**
     * 내부 FollowStatus를 PublicFollowState로 매핑한다.
     *
     * - null 이거나 FOLLOWING/PENDING 이외의 상태(REJECTED, CANCELLED, BLOCKED 등)는 NONE으로 통합한다.
     */
    public static PublicFollowState from(FollowStatus status) {
        if (status == null) {
            return NONE;
        }
        return switch (status) {
            case FOLLOWING -> FOLLOWING;
            case PENDING -> PENDING;
            default -> NONE;
        };
    }
}



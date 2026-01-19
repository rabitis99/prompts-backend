package org.example.sharedprompts.domain.follow;

public enum FollowStatus {
    PENDING,    // 팔로우 요청 대기
    FOLLOWING,  // 팔로우 확정
    REJECTED,   // 요청 거절됨
    CANCELLED,  // 사용자가 요청 취소
    BLOCKED;    // 차단
}


package org.example.sharedprompts.domain.follow;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.global.entity.BaseEntity;

@Entity
@Table(
    name = "follows",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"follower_id", "following_id"})
    },
    indexes = {
        // 추가된 인덱스 목록 (우선순위: 필수)
        // 팔로잉 목록 조회 최적화
        @Index(name = "idx_follows_follower_status", columnList = "follower_id, status"),
        // 팔로워 목록 조회 최적화
        @Index(name = "idx_follows_following_status", columnList = "following_id, status"),
        // 상태별 통계 조회 최적화
        @Index(name = "idx_follows_status_created_at", columnList = "status, created_at"),
        // 팔로워/팔로잉 목록 조회 시 정렬 최적화
        @Index(name = "idx_follows_follower_status_created_at", columnList = "follower_id, status, created_at"),
        @Index(name = "idx_follows_following_status_created_at", columnList = "following_id, status, created_at")
    }
)
@Getter
@NoArgsConstructor
public class Follow extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "follower_id", nullable = false, updatable = false)
    private Long followerId;

    @Column(name = "following_id", nullable = false, updatable = false)
    private Long followingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FollowStatus status;

    public Follow(Long followerId, Long followingId) {
        this.followerId = followerId;
        this.followingId = followingId;
        this.status = FollowStatus.PENDING;
    }

    public void markFollowing() {
        this.status = FollowStatus.FOLLOWING;
    }

    public void markBlocked() {
        this.status = FollowStatus.BLOCKED;
    }

    public void markRejected() {
        this.status = FollowStatus.REJECTED;
    }

    public void markCancelled() {
        this.status = FollowStatus.CANCELLED;
    }

    public void markPending() {
        this.status = FollowStatus.PENDING;
    }
}


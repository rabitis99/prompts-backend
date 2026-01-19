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


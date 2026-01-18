package org.example.sharedprompts.domain.follow;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "follows",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"follower_id", "following_id"})
    }
)
@Getter
@NoArgsConstructor
public class Follow {

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
        this.followerId = (followerId != null) ? followerId : -1L;
        this.followingId = (followingId != null) ? followingId : -1L;
        this.status = FollowStatus.PENDING;
    }

    public void markFollowing() {
        if (followerId <= 0 || followingId <= 0) return;
        this.status = FollowStatus.FOLLOWING;
    }

    public void markBlocked() {
        if (followerId <= 0 || followingId <= 0) return;
        this.status = FollowStatus.BLOCKED;
    }
}


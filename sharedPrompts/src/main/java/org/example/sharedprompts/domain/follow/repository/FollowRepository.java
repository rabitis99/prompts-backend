package org.example.sharedprompts.domain.follow.repository;

import org.example.sharedprompts.domain.follow.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long>, CustomFollowRepository {
    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);
    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);
}


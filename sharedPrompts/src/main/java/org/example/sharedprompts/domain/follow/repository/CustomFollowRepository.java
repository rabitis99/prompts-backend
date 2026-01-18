package org.example.sharedprompts.domain.follow.repository;

import org.example.sharedprompts.domain.follow.FollowStatus;
import org.example.sharedprompts.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomFollowRepository {
    Page<User> findFollowersByUserIdAndStatus(Long userId, FollowStatus status, Pageable pageable);
    Page<User> findFollowingByUserIdAndStatus(Long userId, FollowStatus status, Pageable pageable);
    Long countFollowersByUserIdAndStatus(Long userId, FollowStatus status);
    Long countFollowingByUserIdAndStatus(Long userId, FollowStatus status);
}


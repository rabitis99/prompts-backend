package org.example.sharedprompts.domain.follow.repository.admin;

import org.example.sharedprompts.domain.follow.FollowStatus;
import org.example.sharedprompts.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Admin 전용 Follow 조회용 Repository.
 *
 * - followerId / followingId / status 조합으로 자유롭게 조회 가능
 * - status는 필수이며, null인 경우 전체 테이블 스캔을 방지하기 위해 예외 또는 빈 결과를 반환하도록 설계한다.
 */
public interface AdminFollowRepository {

    Page<User> findUsersByCondition(
            Long followerId,
            Long followingId,
            FollowStatus status,
            Pageable pageable
    );
}



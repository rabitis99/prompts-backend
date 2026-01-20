package org.example.sharedprompts.domain.follow.repository;

import org.example.sharedprompts.domain.follow.Follow;
import org.example.sharedprompts.domain.follow.repository.user.UserFollowRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * User UX 기준의 FollowRepository.
 *
 * - 기본 CRUD + UserFollowRepository(UX 조회용)만 조합한다.
 * - Admin용 조회/통계는 별도의 AdminFollowRepository를 사용한다.
 */
public interface FollowRepository extends JpaRepository<Follow, Long>, UserFollowRepository {
}


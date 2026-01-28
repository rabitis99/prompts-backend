package org.example.sharedprompts.domain.user.service;

import org.example.sharedprompts.domain.user.User;

/**
 * User 엔티티 조회를 담당하는 서비스 인터페이스
 * 
 * 여러 도메인 서비스에서 공통으로 사용되는 User 조회 로직을 중앙화합니다.
 */
public interface UserLookupService {

    /**
     * 사용자 ID로 User 엔티티를 조회합니다.
     * 
     * @param userId 사용자 ID
     * @return User 엔티티
     * @throws org.example.sharedprompts.global.exception.ApiException 사용자를 찾을 수 없는 경우
     */
    User findById(Long userId);

    /**
     * 사용자 ID로 User 엔티티를 조회합니다 (삭제되지 않은 사용자만).
     * 
     * @param userId 사용자 ID
     * @return User 엔티티
     * @throws org.example.sharedprompts.global.exception.ApiException 사용자를 찾을 수 없는 경우
     */
    User findByIdAndNotDeleted(Long userId);
}


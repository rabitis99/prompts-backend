package org.example.sharedprompts.global.redis;

/**
 * 사용자별 tokenVersion을 관리하는 캐시 서비스
 * Redis 기반으로 사용자 상태 변경(soft delete, 차단 등) 시 tokenVersion을 증가시켜
 * 기존 JWT를 무효화합니다.
 */
public interface TokenVersionCacheService {

    /**
     * 사용자의 현재 tokenVersion 조회
     * @param userId 사용자 ID
     * @return 현재 tokenVersion (없으면 0)
     */
    Long getTokenVersion(Long userId);

    /**
     * 사용자의 tokenVersion 증가
     * soft delete, 차단 등 상태 변경 시 호출
     * @param userId 사용자 ID
     */
    void incrementTokenVersion(Long userId);

    /**
     * 사용자의 tokenVersion 초기화 (회원가입 시)
     * @param userId 사용자 ID
     */
    void initializeTokenVersion(Long userId);

    /**
     * 사용자의 tokenVersion 삭제
     * @param userId 사용자 ID
     */
    void deleteTokenVersion(Long userId);
}





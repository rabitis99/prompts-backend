package org.example.sharedprompts.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.auth.storage.RefreshTokenStore;
import org.example.sharedprompts.auth.storage.TokenVersionStore;
import org.springframework.stereotype.Service;

/**
 * 사용자 보안 이벤트 처리 서비스
 * 
 * 사용자 상태 변경 시 토큰 무효화를 담당합니다.
 * - 비밀번호 변경
 * - 계정 삭제
 * - 계정 차단/해제
 * - 강제 로그아웃
 * - OAuth2 계정 연결
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSecurityEvents {

    private final TokenVersionStore tokenVersionStore;
    private final RefreshTokenStore refreshTokenStore;

    /**
     * 비밀번호 변경 시 토큰 무효화
     */
    public void onPasswordChanged(Long userId) {
        tokenVersionStore.increment(userId);
        log.info("비밀번호 변경으로 인한 토큰 무효화: userId={}", userId);
    }

    /**
     * 계정 삭제 시 토큰 무효화 및 삭제
     */
    public void onAccountDeleted(Long userId) {
        tokenVersionStore.delete(userId);
        log.info("계정 삭제로 인한 토큰 무효화 및 삭제: userId={}", userId);
    }

    /**
     * 계정 차단 시 토큰 무효화
     */
    public void onAccountBlocked(Long userId) {
        tokenVersionStore.increment(userId);
        refreshTokenStore.deleteAllByUser(userId);
        log.info("계정 차단으로 인한 토큰 무효화: userId={}", userId);
    }

    /**
     * 계정 차단 해제 시 토큰 무효화
     */
    public void onAccountUnblocked(Long userId) {
        tokenVersionStore.increment(userId);
        refreshTokenStore.deleteAllByUser(userId);
        log.info("계정 차단 해제로 인한 토큰 무효화: userId={}", userId);
    }

    /**
     * 강제 로그아웃 시 토큰 무효화
     */
    public void onForcedLogout(Long userId) {
        tokenVersionStore.increment(userId);
        refreshTokenStore.deleteAllByUser(userId);
        log.info("강제 로그아웃으로 인한 토큰 무효화: userId={}", userId);
    }

    /**
     * 권한 변경 시 토큰 무효화
     */
    public void onRoleChanged(Long userId) {
        tokenVersionStore.increment(userId);
        log.info("권한 변경으로 인한 토큰 무효화: userId={}", userId);
    }

    /**
     * OAuth2 계정 연결 시 토큰 무효화
     */
    public void onOAuth2AccountLinked(Long userId) {
        tokenVersionStore.increment(userId);
        log.info("OAuth2 계정 연결로 인한 토큰 무효화: userId={}", userId);
    }

    /**
     * 사용자 등록 시 tokenVersion 초기화
     */
    public void onUserRegistered(Long userId) {
        tokenVersionStore.initialize(userId);
        log.info("사용자 등록으로 인한 tokenVersion 초기화: userId={}", userId);
    }
}


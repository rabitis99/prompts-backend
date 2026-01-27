package org.example.sharedprompts.domain.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.auth.security.token.TokenSecurityCheckResult;
import org.example.sharedprompts.auth.security.token.TokenSecurityResultHandler;
import org.example.sharedprompts.auth.security.token.TokenSecurityValidator;
import org.example.sharedprompts.auth.storage.RefreshTokenMetadata;
import org.springframework.stereotype.Service;

/**
 * 토큰 보안 검증 관련 로직을 통합하는 서비스
 * 
 * Refresh Token의 IP/User-Agent 검증 및 결과 처리를 담당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenSecurityService {

    private final TokenSecurityValidator tokenSecurityValidator;
    private final TokenSecurityResultHandler tokenSecurityResultHandler;

    /**
     * 토큰 보안 검증 및 결과 처리
     * 
     * @param metadata Refresh Token 메타데이터
     * @param request HTTP 요청
     * @throws org.example.sharedprompts.global.exception.ApiException 보안 위협이 감지된 경우 (MISMATCH)
     */
    public void validateAndHandle(RefreshTokenMetadata metadata, HttpServletRequest request) {
        // 보안 검증 수행
        TokenSecurityCheckResult checkResult = tokenSecurityValidator.validate(metadata, request);
        
        // 검증 결과 처리 (보안 위협 감지 시 예외 발생)
        tokenSecurityResultHandler.handleSecurityCheckResult(checkResult, metadata, request);
    }
}


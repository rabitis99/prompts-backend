package org.example.sharedprompts.auth.security.token;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.auth.audit.AuthAuditPublisher;
import org.example.sharedprompts.auth.storage.RefreshTokenMetadata;
import org.example.sharedprompts.auth.storage.RefreshTokenStore;
import org.example.sharedprompts.domain.audit.auth.enums.AuthFailReason;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

/**
 * 토큰 보안 검증 결과 처리 서비스
 * 
 * TokenSecurityValidator의 검증 결과를 처리하고,
 * 보안 위협이 감지된 경우 적절한 조치를 수행합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenSecurityResultHandler {

    private final RefreshTokenStore refreshTokenStore;
    private final AuthAuditPublisher authAuditPublisher;

    /**
     * 보안 검증 결과 처리
     * 
     * @param checkResult 보안 검증 결과
     * @param metadata Refresh Token 메타데이터
     * @param request HTTP 요청
     * @throws ApiException 보안 위협이 감지된 경우 (MISMATCH)
     */
    public void handleSecurityCheckResult(
            TokenSecurityCheckResult checkResult,
            RefreshTokenMetadata metadata,
            HttpServletRequest request
    ) {
        if (checkResult == TokenSecurityCheckResult.MISMATCH) {
            // 보안 로그 + 전체 세션 무효화
            log.warn("Token refresh security alert - userId={}, storedIp={}, storedUa={}",
                    metadata.getUserId(), metadata.getIp(), metadata.getUserAgent());
            refreshTokenStore.deleteAllByUser(metadata.getUserId());
            authAuditPublisher.tokenRefreshFailByUserId(
                    metadata.getUserId(), 
                    AuthFailReason.INVALID_REFRESH_TOKEN, 
                    request
            );
            throw new ApiException(ErrorCode.REFRESH_TOKEN_SECURITY_MISMATCH);
        } else if (checkResult == TokenSecurityCheckResult.SUSPICIOUS) {
            // 경고 로그만
            log.warn("Suspicious token refresh: userId={}, ip={}, ua={}",
                    metadata.getUserId(), metadata.getIp(), metadata.getUserAgent());
        }
        // MATCH인 경우 아무 작업도 수행하지 않음
    }
}


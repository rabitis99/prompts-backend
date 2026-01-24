package org.example.sharedprompts.auth.security.token;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.auth.storage.RefreshTokenMetadata;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.util.HttpRequestUtils;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 토큰 보안 검증 서비스
 * 
 * Refresh Token의 IP/User-Agent 검증을 담당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenSecurityValidator {

    /**
     * Refresh Token 메타데이터와 현재 요청의 보안 검증
     * 
     * @param metadata 저장된 Refresh Token 메타데이터
     * @param request 현재 HTTP 요청
     * @return 검증 결과
     * @throws ApiException metadata가 null인 경우
     */
    public TokenSecurityCheckResult validate(RefreshTokenMetadata metadata, HttpServletRequest request) {
        if (metadata == null) {
            log.warn("RefreshTokenMetadata가 null입니다. 보안 검증을 수행할 수 없습니다.");
            throw new ApiException(ErrorCode.BAD_REQUEST, "RefreshTokenMetadata는 null일 수 없습니다.");
        }
        
        String currentIp = HttpRequestUtils.getClientIpAddress(request);
        String currentUserAgent = HttpRequestUtils.getUserAgent(request);
        
        return checkTokenSecurity(
                metadata.getIp(), metadata.getUserAgent(),
                currentIp, currentUserAgent
        );
    }
    
    /**
     * 토큰 보안 검증 (IP/User-Agent)
     */
    private TokenSecurityCheckResult checkTokenSecurity(
            String storedIp, String storedUserAgent,
            String currentIp, String currentUserAgent
    ) {
        boolean ipMatch = Objects.equals(storedIp, currentIp);
        boolean uaMatch = Objects.equals(storedUserAgent, currentUserAgent);
        
        if (ipMatch && uaMatch) {
            return TokenSecurityCheckResult.MATCH;
        } else if (isSimilarIp(storedIp, currentIp) || isSimilarUserAgent(storedUserAgent, currentUserAgent)) {
            return TokenSecurityCheckResult.SUSPICIOUS;
        } else {
            return TokenSecurityCheckResult.MISMATCH;
        }
    }
    
    /**
     * IP 유사성 검사 (서브넷 기반)
     * 
     * IPv4의 경우 앞 3옥텟이 같으면 같은 서브넷으로 간주
     */
    private boolean isSimilarIp(String storedIp, String currentIp) {
        if (storedIp == null || currentIp == null || storedIp.isEmpty() || currentIp.isEmpty()) {
            return false;
        }
        
        // IPv4 서브넷 비교
        if (storedIp.contains(".") && currentIp.contains(".")) {
            String[] storedParts = storedIp.split("\\.");
            String[] currentParts = currentIp.split("\\.");
            
            if (storedParts.length == 4 && currentParts.length == 4) {
                try {
                    // 앞 3옥텟이 같으면 유사 (같은 서브넷)
                    return storedParts[0].equals(currentParts[0]) &&
                           storedParts[1].equals(currentParts[1]) &&
                           storedParts[2].equals(currentParts[2]);
                } catch (Exception e) {
                    log.debug("IP 비교 중 오류: storedIp={}, currentIp={}", storedIp, currentIp, e);
                    return false;
                }
            }
        }
        
        // IPv6는 현재 정확한 일치만 허용
        return false;
    }
    
    /**
     * User-Agent 유사성 검사 (브라우저 기반)
     * 
     * 브라우저 이름이 같으면 유사로 간주
     */
    private boolean isSimilarUserAgent(String storedUa, String currentUa) {
        if (storedUa == null || currentUa == null || storedUa.isEmpty() || currentUa.isEmpty()) {
            return false;
        }
        
        String storedBrowser = extractBrowser(storedUa);
        String currentBrowser = extractBrowser(currentUa);
        
        return storedBrowser != null && 
               currentBrowser != null && 
               storedBrowser.equals(currentBrowser);
    }
    
    /**
     * User-Agent에서 브라우저 이름 추출
     */
    private String extractBrowser(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        
        String ua = userAgent.toLowerCase();
        if (ua.contains("chrome") && !ua.contains("edg")) {
            return "Chrome";
        }
        if (ua.contains("firefox")) {
            return "Firefox";
        }
        if (ua.contains("safari") && !ua.contains("chrome")) {
            return "Safari";
        }
        if (ua.contains("edg")) {
            return "Edge";
        }
        if (ua.contains("opera") || ua.contains("opr")) {
            return "Opera";
        }
        
        return null;
    }
}



package org.example.sharedprompts.global.util;

import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.util.StringUtils;

public class JwtTokenExtractor {

    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Authorization 헤더 값에서 JWT Access Token을 추출합니다.
     *
     * @param authorizationHeader Authorization 헤더 값 (예: "Bearer {token}")
     * @return 추출된 JWT 토큰 (Bearer 제외)
     * @throws ApiException Authorization 헤더가 null이거나 잘못된 형식인 경우
     */
    public static String extractAccessToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        
        return authorizationHeader.substring(BEARER_PREFIX.length());
    }
}


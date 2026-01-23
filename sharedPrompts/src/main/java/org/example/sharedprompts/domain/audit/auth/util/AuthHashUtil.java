package org.example.sharedprompts.domain.audit.auth.util;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 인증 이벤트 로그에서 개인정보 보호를 위한 해시 유틸리티
 * 
 * <p>providerId나 email 같은 민감한 정보는 HMAC-SHA256으로 해시 후 저장합니다.
 * 서버 측 비밀키(pepper)를 사용하여 사전 공격으로부터 보호합니다.
 * 
 * <p>OAuth2StateService와 동일한 HMAC secret을 사용합니다.
 */
@Slf4j
@Component
public class AuthHashUtil {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    
    private final String hmacSecret;
    
    public AuthHashUtil(@Value("${oauth2.salt}") String hmacSecret) {
        this.hmacSecret = hmacSecret;
    }

    /**
     * 문자열을 HMAC-SHA256으로 해시
     * 
     * <p>서버 측 비밀키(pepper)를 사용하여 사전 공격으로부터 보호합니다.
     * 이메일이나 providerId 같은 낮은 엔트로피 데이터의 역추적을 방지합니다.
     * 
     * @param input 해시할 문자열
     * @return Base64 URL 인코딩된 HMAC 해시 문자열, null이면 null 반환
     */
    public String hash(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    hmacSecret.getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            );
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().encodeToString(hmacBytes);
        } catch (Exception e) {
            log.error("HMAC-SHA256 해시 계산 실패", e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, null, "HMAC-SHA256 해시 계산 실패", e);
        }
    }

    /**
     * byte 배열을 HMAC-SHA256으로 해시
     * 
     * <p>문자열 변환 없이 직접 해시하므로 비UTF-8 payload에서도 안전하고,
     * 대용량 메시지에서 불필요한 메모리 할당을 방지합니다.
     * 서버 측 비밀키(pepper)를 사용하여 사전 공격으로부터 보호합니다.
     * 
     * @param input 해시할 byte 배열
     * @return Base64 URL 인코딩된 HMAC 해시 문자열, null이거나 비어있으면 null 반환
     */
    public String hash(byte[] input) {
        if (input == null || input.length == 0) {
            return null;
        }

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    hmacSecret.getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            );
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(input);
            return Base64.getUrlEncoder().encodeToString(hmacBytes);
        } catch (Exception e) {
            log.error("HMAC-SHA256 해시 계산 실패", e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, null, "HMAC-SHA256 해시 계산 실패", e);
        }
    }
}


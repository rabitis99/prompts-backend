package org.example.sharedprompts.domain.audit.auth.util;

import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 인증 이벤트 로그에서 개인정보 보호를 위한 해시 유틸리티
 * providerId나 email 같은 민감한 정보는 해시 후 저장합니다.
 */
public class AuthHashUtil {

    private static final String ALGORITHM = "SHA-256";

    /**
     * 문자열을 SHA-256으로 해시
     * 
     * @param input 해시할 문자열
     * @return 16진수 해시 문자열, null이면 null 반환
     */
    public static String hash(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            
            // 16진수 문자열로 변환
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256은 표준 알고리즘이므로 발생하지 않아야 함
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, null, "SHA-256 algorithm not found", e);
        }
    }
}


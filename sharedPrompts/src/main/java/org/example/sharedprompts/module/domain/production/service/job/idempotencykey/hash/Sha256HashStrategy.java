package org.example.sharedprompts.module.domain.production.service.job.idempotencykey.hash;

import org.example.sharedprompts.module.domain.production.service.job.idempotencykey.exception.IdempotencyKeyGenerationException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * SHA-256 해시 전략 구현체
 */
@Component
public class Sha256HashStrategy implements HashStrategy {

    private static final String ALGORITHM = "SHA-256";

    @Override
    public String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (Exception e) {
            throw new IdempotencyKeyGenerationException(
                    "Failed to generate SHA-256 hash: " + e.getMessage(), e);
        }
    }

    /**
     * 바이트 배열을 16진수 문자열로 변환
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}


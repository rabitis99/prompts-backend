package org.example.sharedprompts.module.domain.github;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Verifies GitHub webhook payload using X-Hub-Signature-256 (HMAC-SHA256).
 * GitHub sends the header as "sha256=&lt;hex_digest&gt;". Comparison is constant-time to prevent timing attacks.
 * 시크릿은 호출부에서 {@link GitHubWebhookSecretResolver}로 tenant별 조회 후 전달하는 것을 권장합니다.
 */
@Component
@Slf4j
public class GitHubWebhookSignatureVerifier {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String PREFIX = "sha256=";

    /**
     * Verifies that the given signature header matches HMAC-SHA256(secret, payload).
     * 시크릿은 DB(테넌트별 webhook_secret) 또는 전역 설정에서 조회한 값을 넘깁니다.
     *
     * @param payload            raw request body (UTF-8)
     * @param signature256Header value of X-Hub-Signature-256 (e.g. "sha256=abc123...")
     * @param secret             사용할 webhook secret (null/blank면 false)
     * @return true if secret is non-blank, signature is present and valid; false otherwise
     */
    public boolean verify(String payload, String signature256Header, String secret) {
        if (secret == null || secret.isBlank()) {
            log.debug("GitHub webhook secret not provided for verification");
            return false;
        }
        if (signature256Header == null || signature256Header.isBlank()) {
            log.debug("X-Hub-Signature-256 missing");
            return false;
        }
        if (!signature256Header.toLowerCase().startsWith(PREFIX)) {
            log.debug("X-Hub-Signature-256 invalid format (expected sha256=...)");
            return false;
        }
        String receivedHex = signature256Header.substring(PREFIX.length()).trim();
        byte[] expectedBytes = hmacSha256(secret, payload != null ? payload : "");
        if (expectedBytes == null) {
            return false;
        }
        String expectedHex = HexFormat.of().formatHex(expectedBytes);
        return constantTimeEquals(expectedHex, receivedHex);
    }

    private static byte[] hmacSha256(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("HMAC-SHA256 failed: {}", e.getMessage());
            return null;
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        if (aBytes.length != bBytes.length) {
            return false;
        }
        return MessageDigest.isEqual(aBytes, bBytes);
    }
}

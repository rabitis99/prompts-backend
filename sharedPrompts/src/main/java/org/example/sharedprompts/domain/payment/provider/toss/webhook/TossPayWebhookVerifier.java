package org.example.sharedprompts.domain.payment.provider.toss.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.properties.TossPayProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * TossPay Webhook Verifier
 * 
 * <p>단일 책임: Webhook 서명 검증만 담당
 * - 상태 변경 없음
 * - Null 안전성 보장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TossPayWebhookVerifier {

    private final TossPayProperties properties;

    /**
     * Webhook 서명 검증
     */
    public boolean verify(String payload, String signature) {
        if (payload == null || payload.isEmpty()) {
            log.warn("TossPay Webhook 검증 실패: payload가 비어있습니다");
            return false;
        }
        if (signature == null || signature.isEmpty()) {
            log.warn("TossPay Webhook 검증 실패: signature가 비어있습니다");
            return false;
        }

        try {
            String secret = properties.getSecretKey();
            if (secret == null || secret.isEmpty()) {
                log.warn("TossPay Webhook secret이 설정되지 않았습니다");
                return false;
            }

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            byte[] expectedSignature = Base64.getDecoder().decode(signature);

            boolean verified = MessageDigest.isEqual(hash, expectedSignature);
            if (!verified) {
                log.warn("TossPay Webhook 서명 검증 실패");
            }
            return verified;
        } catch (Exception e) {
            log.error("TossPay Webhook 서명 검증 실패: error={}", e.getMessage(), e);
            return false;
        }
    }
}


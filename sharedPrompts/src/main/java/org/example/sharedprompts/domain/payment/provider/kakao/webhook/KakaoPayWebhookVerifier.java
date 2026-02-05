package org.example.sharedprompts.domain.payment.provider.kakao.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.properties.WebhookProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoPayWebhookVerifier {

    private final WebhookProperties properties;

    public boolean verify(String payload, String signature) {
        try {
            String secret = properties.getSecret();
            if (secret == null || secret.isEmpty()) {
                log.error("Webhook secret이 설정되지 않았습니다");
                throw new IllegalStateException("Webhook secret이 설정되지 않았습니다");
            }

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            ));

            byte[] expected = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            byte[] actual = Base64.getDecoder().decode(signature);

            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Webhook signature verification failed: {}", e.getMessage());
            return false;
        }
    }
}

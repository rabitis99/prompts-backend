package org.example.sharedprompts.module.domain.github;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GitHubWebhookSignatureVerifier")
class GitHubWebhookSignatureVerifierTest {

    private static String hmacSha256Hex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final GitHubWebhookSignatureVerifier verifier = new GitHubWebhookSignatureVerifier();

    @Nested
    @DisplayName("secret 미설정 (null/blank)")
    class WhenSecretNotProvided {

        @Test
        @DisplayName("secret이 null이면 verify false")
        void secretNull_returnsFalse() {
            assertThat(verifier.verify("{}", "sha256=abc", null)).isFalse();
        }

        @Test
        @DisplayName("secret이 blank면 verify false")
        void secretBlank_returnsFalse() {
            assertThat(verifier.verify("{}", "sha256=abc", "")).isFalse();
        }
    }

    @Nested
    @DisplayName("secret 제공됨")
    class WhenSecretProvided {

        @Test
        @DisplayName("유효한 X-Hub-Signature-256이면 verify true")
        void validSignature_returnsTrue() {
            String secret = "my-webhook-secret";
            String payload = "{\"ref\":\"refs/heads/main\"}";
            String sig = "sha256=" + hmacSha256Hex(secret, payload);
            assertThat(verifier.verify(payload, sig, secret)).isTrue();
        }

        @Test
        @DisplayName("서명 불일치면 verify false")
        void wrongSignature_returnsFalse() {
            assertThat(verifier.verify("{}", "sha256=deadbeef", "secret")).isFalse();
        }

        @Test
        @DisplayName("X-Hub-Signature-256 없으면 verify false")
        void missingHeader_returnsFalse() {
            assertThat(verifier.verify("{}", null, "secret")).isFalse();
            assertThat(verifier.verify("{}", "", "secret")).isFalse();
        }

        @Test
        @DisplayName("sha256= 접두어 없으면 verify false")
        void wrongPrefix_returnsFalse() {
            String validSig = "sha256=" + hmacSha256Hex("secret", "{}");
            assertThat(verifier.verify("{}", "sha1=" + validSig.substring(6), "secret")).isFalse();
        }
    }
}

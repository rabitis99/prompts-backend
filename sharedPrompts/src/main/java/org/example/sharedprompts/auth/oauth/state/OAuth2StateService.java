package org.example.sharedprompts.auth.oauth.state;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.auth.redis.RedisKeyFactory;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.util.RandomGenerator;
import org.example.sharedprompts.global.util.SecurityUtils;
import org.example.sharedprompts.global.util.SensitiveDataMasker;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * OAuth2 State 검증 서비스
 * 
 * State 값의 무결성과 1회용 보장을 위해 HMAC-SHA256과 Redis를 사용합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2StateService {

    private final StringRedisTemplate redisTemplate;
    private final OAuth2StateProperties properties;
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * OAuth2 State 생성
     * 
     * 형식: Base64(nonce:timestamp:hmac)
     * - nonce: UUID (랜덤 값)
     * - timestamp: 생성 시각 (밀리초)
     * - hmac: HMAC-SHA256(nonce:timestamp)
     * 
     * @return 생성된 State 값
     */
    public String generateState() {
        String nonce = RandomGenerator.randomUUID();
        long timestamp = System.currentTimeMillis();
        String data = nonce + ":" + timestamp;
        String hmac = calculateHmac(data);
        String state = Base64.getUrlEncoder().encodeToString(
                (data + ":" + hmac).getBytes(StandardCharsets.UTF_8)
        );

        // Redis에 저장 (1회용 보장)
        String key = RedisKeyFactory.oauthState(state);
        redisTemplate.opsForValue().set(
                key,
                "1",
                Duration.ofMinutes(properties.getStateValidityMinutes())
        );

        return state;
    }

    /**
     * OAuth2 State 검증 및 소비 (1회용)
     * 
     * @param state 검증할 State 값
     * @return 검증 성공 여부
     */
    public boolean validateAndConsume(String state) {
        if (state == null || state.isEmpty()) {
            return false;
        }

        String key = RedisKeyFactory.oauthState(state);

        // 1. Redis에서 조회 및 삭제 (1회용 보장)
        String value = redisTemplate.opsForValue().getAndDelete(key);
        if (value == null) {
            log.debug("OAuth2 State 검증 실패: Redis에 없거나 이미 사용됨 - state={}", SensitiveDataMasker.maskToken(state));
            return false; // 이미 사용되었거나 만료됨
        }

        // 2. 형식 검증
        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(state),
                    StandardCharsets.UTF_8
            );
            String[] parts = decoded.split(":");
            if (parts.length != 3) {
                log.debug("OAuth2 State 형식 오류: parts.length={}, state={}", parts.length, SensitiveDataMasker.maskToken(state));
                return false;
            }

            String nonce = parts[0];
            long timestamp = Long.parseLong(parts[1]);
            String hmac = parts[2];

            // 3. 타임스탬프 검증
            long age = System.currentTimeMillis() - timestamp;
            if (age > properties.getStateValidityMillis()) {
                log.debug("OAuth2 State 만료: age={}ms, validity={}ms, state={}", 
                        age, properties.getStateValidityMillis(), SensitiveDataMasker.maskToken(state));
                return false;
            }

            // 4. HMAC 검증 (타이밍 공격 방지를 위한 상수 시간 비교)
            String data = nonce + ":" + timestamp;
            String expectedHmac = calculateHmac(data);
            boolean isValid = SecurityUtils.constantTimeEquals(hmac, expectedHmac);
            
            if (!isValid) {
                log.warn("OAuth2 State HMAC 검증 실패: state={}", SensitiveDataMasker.maskToken(state));
            }
            
            return isValid;
        } catch (Exception e) {
            log.debug("OAuth2 State 검증 중 예외 발생: state={}, error={}", SensitiveDataMasker.maskToken(state), e.getMessage());
            return false;
        }
    }

    /**
     * HMAC-SHA256 계산
     * 
     * @param data 원본 데이터
     * @return Base64 인코딩된 HMAC 값
     */
    private String calculateHmac(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    properties.getHmacSecret().getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            );
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().encodeToString(hmacBytes);
        } catch (Exception e) {
            log.error("HMAC 계산 실패", e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "HMAC 계산 실패", e);
        }
    }

}


package org.example.sharedprompts.auth.jwt.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Getter
@Component
public class TokenTtlProperties {

    private final long accessTokenValidityMinutes;
    private final long refreshTokenValidityDays;
    private final long refreshTokenRotationThresholdDays;

    public TokenTtlProperties(
            @Value("${jwt.access.expiration}") long accessTokenValidityMinutes,
            @Value("${jwt.refresh.expiration}") long refreshTokenValidityDays,
            @Value("${jwt.refresh.rotation-threshold-days:1}") long refreshTokenRotationThresholdDays
    ) {
        this.accessTokenValidityMinutes = accessTokenValidityMinutes;
        this.refreshTokenValidityDays = refreshTokenValidityDays;
        this.refreshTokenRotationThresholdDays = refreshTokenRotationThresholdDays;
    }

    /**
     * Access Token 유효 기간을 밀리초로 변환
     */
    public long getAccessTokenValidityMillis() {
        return TimeUnit.MINUTES.toMillis(accessTokenValidityMinutes);
    }

    /**
     * Refresh Token 유효 기간을 밀리초로 변환
     */
    public long getRefreshTokenValidityMillis() {
        return TimeUnit.DAYS.toMillis(refreshTokenValidityDays);
    }

    /**
     * Refresh Token 회전 임계값을 밀리초로 변환
     */
    public long getRefreshTokenRotationThresholdMillis() {
        return TimeUnit.DAYS.toMillis(refreshTokenRotationThresholdDays);
    }
}


package org.example.sharedprompts.auth.oauth.state;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * OAuth2 State 검증 설정 프로퍼티
 */
@Getter
@Component
public class OAuth2StateProperties {

    private final long stateValidityMinutes;
    private final String hmacSecret;

    public OAuth2StateProperties(
            @Value("${oauth2.state.validity-minutes:10}") long stateValidityMinutes,
            @Value("${oauth2.salt}") String hmacSecret
    ) {
        this.stateValidityMinutes = stateValidityMinutes;
        this.hmacSecret = hmacSecret;
    }

    /**
     * State 유효 기간을 밀리초로 변환
     */
    public long getStateValidityMillis() {
        return TimeUnit.MINUTES.toMillis(stateValidityMinutes);
    }
}


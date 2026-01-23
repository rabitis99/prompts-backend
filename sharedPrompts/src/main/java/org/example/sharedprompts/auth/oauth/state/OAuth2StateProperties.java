package org.example.sharedprompts.auth.oauth.state;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * OAuth2 State 검증 설정 프로퍼티
 * 
 * <p>OAuth2 State의 유효 기간을 관리합니다.
 * HMAC secret은 AuthHashUtil에서 관리하므로 이 클래스에서는 제외됩니다.
 */
@Getter
@Component
public class OAuth2StateProperties {

    private final long stateValidityMinutes;

    public OAuth2StateProperties(
            @Value("${oauth2.state.validity-minutes:10}") long stateValidityMinutes
    ) {
        this.stateValidityMinutes = stateValidityMinutes;
    }

    /**
     * State 유효 기간을 밀리초로 변환
     */
    public long getStateValidityMillis() {
        return TimeUnit.MINUTES.toMillis(stateValidityMinutes);
    }
}


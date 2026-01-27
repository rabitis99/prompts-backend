package org.example.sharedprompts.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 프롬프트 생성 관련 설정 속성
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.prompt.creation")
public class PromptCreationProperties {
    /**
     * 프롬프트 생성 타임아웃 (밀리초)
     * 기본값: 40000ms (40초)
     */
    private long timeoutMs = 40_000L;

    /**
     * Future 타임아웃 (밀리초)
     * WebAsyncTask 타임아웃보다 1초 짧게 설정
     */
    public long getFutureTimeoutMs() {
        return timeoutMs - 1_000L;
    }
}


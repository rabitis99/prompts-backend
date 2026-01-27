package org.example.sharedprompts.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Min;

/**
 * 프롬프트 생성 관련 설정 속성
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.prompt.creation")
public class PromptCreationProperties {
    /**
     * 프롬프트 생성 타임아웃 (밀리초)
     * 기본값: 40000ms (40초)
     */
    @Min(2000) // Ensure that timeoutMs is at least 2000ms
    private long timeoutMs = 40_000L;

    /**
     * Future 타임아웃 (밀리초)
     * WebAsyncTask 타임아웃보다 1초 짧게 설정
     */
    public long getFutureTimeoutMs() {
        return timeoutMs - 1_000L;
    }
}

package org.example.sharedprompts.auth.rate.policy;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.Min;

/**
 * Rate Limit 윈도우 설정
 */
@Setter
@Getter
public class RateLimitWindows {

    /**
     * 기본 윈도우 시간 (초)
     */
    @Min(1)
    private long defaultSeconds = 60L;

    /**
     * 로그인 윈도우 시간 (초) - 기본값보다 긴 윈도우 적용
     */
    @Min(1)
    private long loginSeconds = 900L;

    /**
     * 회원가입 윈도우 시간 (초)
     */
    @Min(1)
    private long signupSeconds = 3600L;

    /**
     * OAuth2 로그인 확정 윈도우 시간 (초)
     */
    @Min(1)
    private long confirmSeconds = 60L;

    /**
     * 프롬프트 생성 윈도우 시간 (초)
     */
    @Min(1)
    private long promptCreateSeconds = 60L;

    /**
     * 일반 API 윈도우 시간 (초)
     */
    @Min(1)
    private long generalSeconds = 60L;
}


package org.example.sharedprompts.auth.rate.policy;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.auth.rate.policy.matcher.ExactPathMatcher;
import org.example.sharedprompts.auth.rate.policy.matcher.PrefixPathMatcher;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;

import static org.example.sharedprompts.auth.rate.policy.RateLimitConstants.ApiPaths;

/**
 * Rate Limit 규칙 설정
 * 
 * Rate Limit Matcher 목록을 구성합니다.
 * Spring Bean으로 등록하여 의존성 주입이 가능하도록 할 수 있습니다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RateLimitRuleConfig {

    /**
     * Rate Limit Matcher 목록을 생성합니다.
     * 우선순위: 정확한 경로 매칭 → prefix 매칭 순서
     * 
     * @return Rate Limit Matcher 목록
     */
    public static List<RateLimitMatcher> createMatchers() {
        List<RateLimitMatcher> matchers = new ArrayList<>();

        // 정확한 경로 매칭 (우선순위 높음)
        matchers.add(new ExactPathMatcher(
                ApiPaths.AUTH_LOGIN,
                HttpMethod.POST,
                RateLimitRule.Predefined.LOGIN
        ));
        matchers.add(new ExactPathMatcher(
                ApiPaths.AUTH_SIGNUP,
                HttpMethod.POST,
                RateLimitRule.Predefined.SIGNUP
        ));
        matchers.add(new ExactPathMatcher(
                ApiPaths.PROMPTS,
                HttpMethod.POST,
                RateLimitRule.Predefined.PROMPT_CREATE
        ));

        // Prefix 매칭 (우선순위 낮음 - 마지막에 배치)
        matchers.add(new PrefixPathMatcher(
                ApiPaths.API_PREFIX,
                RateLimitRule.Predefined.GENERAL
        ));

        return matchers;
    }
}





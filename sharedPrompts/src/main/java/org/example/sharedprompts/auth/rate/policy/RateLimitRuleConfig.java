package org.example.sharedprompts.auth.rate.policy;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.auth.rate.policy.matcher.ExactPathMatcher;
import org.example.sharedprompts.auth.rate.policy.matcher.PrefixPathMatcher;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.example.sharedprompts.auth.rate.policy.RateLimitConstants.ApiPaths;

/**
 * Rate Limit 규칙 설정
 * 
 * Rate Limit Matcher 목록을 구성합니다.
 * application.yml의 설정을 기반으로 Rate Limit 규칙을 생성합니다.
 */
@Component
@RequiredArgsConstructor
public class RateLimitRuleConfig {

    private final RateLimitProperties properties;

    /**
     * Rate Limit Matcher 목록을 생성합니다.
     * 우선순위: 정확한 경로 매칭 → prefix 매칭 순서
     * 
     * @return Rate Limit Matcher 목록
     */
    public List<RateLimitMatcher> createMatchers() {
        List<RateLimitMatcher> matchers = new ArrayList<>();

        // 정확한 경로 매칭 (우선순위 높음)
        matchers.add(new ExactPathMatcher(
                ApiPaths.AUTH_LOGIN,
                HttpMethod.POST,
                createLoginRule()
        ));
        matchers.add(new ExactPathMatcher(
                ApiPaths.AUTH_SIGNUP,
                HttpMethod.POST,
                createSignupRule()
        ));
        matchers.add(new ExactPathMatcher(
                ApiPaths.AUTH_CONFIRM,
                HttpMethod.POST,
                createConfirmRule()
        ));
        matchers.add(new ExactPathMatcher(
                ApiPaths.PROMPTS,
                HttpMethod.POST,
                createPromptCreateRule()
        ));

        // Prefix 매칭 (우선순위 낮음 - 마지막에 배치)
        matchers.add(new PrefixPathMatcher(
                ApiPaths.API_PREFIX,
                createGeneralRule()
        ));

        return matchers;
    }

    /**
     * 로그인 Rate Limit 규칙 생성
     */
    private RateLimitRule createLoginRule() {
        return new RateLimitRule(
                RateLimitConstants.RuleNames.LOGIN,
                properties.getRules().getLogin(),
                properties.getWindows().getLoginSeconds()
        );
    }

    /**
     * 회원가입 Rate Limit 규칙 생성
     */
    private RateLimitRule createSignupRule() {
        return new RateLimitRule(
                RateLimitConstants.RuleNames.SIGNUP,
                properties.getRules().getSignup(),
                properties.getWindows().getSignupSeconds()
        );
    }

    /**
     * OAuth2 로그인 확정 Rate Limit 규칙 생성
     */
    private RateLimitRule createConfirmRule() {
        return new RateLimitRule(
                RateLimitConstants.RuleNames.CONFIRM,
                properties.getRules().getConfirm(),
                properties.getWindows().getConfirmSeconds()
        );
    }

    /**
     * 프롬프트 생성 Rate Limit 규칙 생성
     */
    private RateLimitRule createPromptCreateRule() {
        return new RateLimitRule(
                RateLimitConstants.RuleNames.PROMPT_CREATE,
                properties.getRules().getPromptCreate(),
                properties.getWindows().getPromptCreateSeconds()
        );
    }

    /**
     * 일반 API Rate Limit 규칙 생성
     */
    private RateLimitRule createGeneralRule() {
        return new RateLimitRule(
                RateLimitConstants.RuleNames.GENERAL,
                properties.getRules().getGeneral(),
                properties.getWindows().getGeneralSeconds()
        );
    }
}







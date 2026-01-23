package org.example.sharedprompts.auth.rate.policy;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Rate Limit 규칙 해석기
 * 
 * URI와 HTTP Method를 기반으로 적절한 Rate Limit 규칙을 결정합니다.
 * 전략 패턴을 사용하여 확장 가능한 구조로 설계되었습니다.
 * Spring Bean으로 관리되어 런타임에 Matcher 목록을 변경할 수 있습니다.
 */
@Component
@RequiredArgsConstructor
@Getter
public class RateLimitRuleResolver {

    /**
     * Rate Limit Matcher 목록
     * 우선순위: 정확한 경로 매칭 → prefix 매칭 순서
     */
    private final List<RateLimitMatcher> matchers;

    /**
     * 기본 생성자 - RateLimitRuleConfig를 사용하여 Matcher 목록 초기화
     */
    public RateLimitRuleResolver() {
        this.matchers = RateLimitRuleConfig.createMatchers();
    }

    /**
     * URI와 HTTP Method를 기반으로 Rate Limit 규칙을 결정합니다.
     * 
     * @param uri 요청 URI
     * @param method HTTP Method
     * @return RateLimitRule (일치하는 규칙이 없으면 null)
     */
    public RateLimitRule resolve(String uri, HttpMethod method) {
        for (RateLimitMatcher matcher : matchers) {
            if (matcher.matches(uri, method)) {
                return matcher.getRule();
            }
        }
        return null;
    }
}


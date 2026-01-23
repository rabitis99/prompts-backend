package org.example.sharedprompts.auth.rate.filter.service.resolution;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.auth.rate.policy.RateLimitRule;
import org.example.sharedprompts.auth.rate.policy.RateLimitRuleResolver;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Rate Limit 규칙 해석 서비스
 * 
 * URI와 HTTP Method를 기반으로 Rate Limit 규칙을 결정합니다.
 */
@Service
@RequiredArgsConstructor
public class RateLimitRuleResolverService {

    private final RateLimitRuleResolver policyRuleResolver;

    /**
     * URI와 HTTP Method를 기반으로 Rate Limit 규칙을 결정합니다.
     * 
     * @param uri 요청 URI
     * @param method HTTP Method
     * @return Optional<RateLimitRule> (일치하는 규칙이 없으면 empty)
     */
    public Optional<RateLimitRule> resolve(String uri, HttpMethod method) {
        RateLimitRule rule = policyRuleResolver.resolve(uri, method);
        return Optional.ofNullable(rule);
    }
}





package org.example.sharedprompts.auth.rate.policy.matcher;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.auth.rate.policy.RateLimitMatcher;
import org.example.sharedprompts.auth.rate.policy.RateLimitRule;
import org.springframework.http.HttpMethod;

/**
 * 정확한 경로와 HTTP Method를 매칭하는 Matcher
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ExactPathMatcher implements RateLimitMatcher {
    
    private String path;
    private HttpMethod method;
    private RateLimitRule rule;
    
    public ExactPathMatcher(String path, HttpMethod method, RateLimitRule rule) {
        this.path = path;
        this.method = method;
        this.rule = rule;
    }
    
    @Override
    public boolean matches(String uri, HttpMethod requestMethod) {
        String normalized = uri;
        int queryIdx = normalized.indexOf('?');
        if (queryIdx >= 0) {
            normalized = normalized.substring(0, queryIdx);
        }
        return method == requestMethod && path.equals(normalized);
    }
    
    @Override
    public RateLimitRule getRule() {
        return rule;
    }
}


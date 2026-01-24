package org.example.sharedprompts.auth.rate.policy.matcher;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.auth.rate.policy.RateLimitMatcher;
import org.example.sharedprompts.auth.rate.policy.RateLimitRule;
import org.example.sharedprompts.global.util.ValidationUtils;
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
        this.path = ValidationUtils.requireNonNull(path, "path");
        this.method = ValidationUtils.requireNonNull(method, "method");
        this.rule = ValidationUtils.requireNonNull(rule, "rule");
    }
    
    @Override
    public boolean matches(String uri, HttpMethod requestMethod) {
        if (uri == null) {
            return false;
        }
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


package org.example.sharedprompts.auth.rate.policy.matcher;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.auth.rate.policy.RateLimitMatcher;
import org.example.sharedprompts.auth.rate.policy.RateLimitRule;
import org.springframework.http.HttpMethod;

/**
 * 정확한 경로와 HTTP Method를 매칭하는 Matcher
 */
@RequiredArgsConstructor
public class ExactPathMatcher implements RateLimitMatcher {
    
    private final String path;
    private final HttpMethod method;
    private final RateLimitRule rule;
    
    @Override
    public boolean matches(String uri, HttpMethod requestMethod) {
        return method == requestMethod && path.equals(uri);
    }
    
    @Override
    public RateLimitRule getRule() {
        return rule;
    }
}


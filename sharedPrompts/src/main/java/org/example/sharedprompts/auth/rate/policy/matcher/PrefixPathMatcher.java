package org.example.sharedprompts.auth.rate.policy.matcher;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.auth.rate.policy.RateLimitMatcher;
import org.example.sharedprompts.auth.rate.policy.RateLimitRule;
import org.springframework.http.HttpMethod;

/**
 * 경로 prefix를 매칭하는 Matcher
 */
@RequiredArgsConstructor
public class PrefixPathMatcher implements RateLimitMatcher {
    
    private final String prefix;
    private final RateLimitRule rule;
    
    @Override
    public boolean matches(String uri, HttpMethod requestMethod) {
        return uri.startsWith(prefix);
    }
    
    @Override
    public RateLimitRule getRule() {
        return rule;
    }
}





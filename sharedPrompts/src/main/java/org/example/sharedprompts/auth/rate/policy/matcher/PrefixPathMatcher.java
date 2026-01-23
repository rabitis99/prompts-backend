package org.example.sharedprompts.auth.rate.policy.matcher;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.auth.rate.policy.RateLimitMatcher;
import org.example.sharedprompts.auth.rate.policy.RateLimitRule;
import org.springframework.http.HttpMethod;

/**
 * 접두사 경로와 HTTP Method를 매칭하는 Matcher
 * 
 * 경로 경계를 보장하여 /api가 /apiary에도 매칭되지 않도록 합니다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PrefixPathMatcher implements RateLimitMatcher {
    
    private String prefix;
    private HttpMethod method;
    private RateLimitRule rule;
    
    public PrefixPathMatcher(String prefix, HttpMethod method, RateLimitRule rule) {
        this.prefix = prefix;
        this.method = method;
        this.rule = rule;
    }
    
    @Override
    public boolean matches(String uri, HttpMethod requestMethod) {
        if (uri == null) {
            return false;
        }
        
        // HTTP Method가 일치하지 않으면 false
        if (method != requestMethod) {
            return false;
        }
        
        // 정확히 일치하는 경우
        if (uri.equals(prefix)) {
            return true;
        }
        
        // 경로 경계 보장: prefix가 /로 끝나지 않으면 /를 추가하여 경계를 명확히 함
        // 예: /api -> /api/로 정규화하여 /apiary와 구분
        String normalizedPrefix = prefix.endsWith("/") ? prefix : prefix + "/";
        return uri.startsWith(normalizedPrefix);
    }
    
    @Override
    public RateLimitRule getRule() {
        return rule;
    }
}

package org.example.sharedprompts.auth.rate.policy.matcher;

import org.example.sharedprompts.auth.rate.policy.RateLimitMatcher;
import org.example.sharedprompts.auth.rate.policy.RateLimitRule;
import org.springframework.http.HttpMethod;

import java.util.Objects;

/**
 * 접두사 경로와 HTTP Method를 매칭하는 Matcher
 * 
 * 경로 경계를 보장하여 /api가 /apiary에도 매칭되지 않도록 합니다.
 * 
 * <p>특징:
 * <ul>
 *   <li>쿼리 스트링 자동 제거: /api?page=1은 /api로 매칭됩니다.</li>
 *   <li>HTTP Method 처리:
 *       <ul>
 *         <li>생성자에 HttpMethod를 지정하지 않으면 모든 HTTP 메서드를 허용합니다.</li>
 *         <li>특정 HTTP 메서드만 허용하려면 생성자에 HttpMethod를 지정합니다.</li>
 *       </ul>
 *   </li>
 * </ul>
 */
public class PrefixPathMatcher implements RateLimitMatcher {
    
    private final String prefix;
    private final HttpMethod method;
    private final RateLimitRule rule;
    
    /**
     * PrefixPathMatcher 생성자 (모든 HTTP 메서드 허용)
     * 
     * @param prefix 경로 접두사 (null이 아니어야 함)
     * @param rule Rate Limit 규칙 (null이 아니어야 함)
     * @throws NullPointerException prefix 또는 rule이 null인 경우
     */
    public PrefixPathMatcher(String prefix, RateLimitRule rule) {
        this.prefix = Objects.requireNonNull(prefix, "prefix must not be null");
        this.method = null; // null이면 모든 HTTP 메서드 허용
        this.rule = Objects.requireNonNull(rule, "rule must not be null");
    }
    
    /**
     * PrefixPathMatcher 생성자 (특정 HTTP 메서드만 허용)
     * 
     * @param prefix 경로 접두사 (null이 아니어야 함)
     * @param method 허용할 HTTP 메서드 (null이면 모든 메서드 허용)
     * @param rule Rate Limit 규칙 (null이 아니어야 함)
     * @throws NullPointerException prefix 또는 rule이 null인 경우
     */
    public PrefixPathMatcher(String prefix, HttpMethod method, RateLimitRule rule) {
        this.prefix = Objects.requireNonNull(prefix, "prefix must not be null");
        this.method = method; // null이면 모든 HTTP 메서드 허용
        this.rule = Objects.requireNonNull(rule, "rule must not be null");
    }
    
    @Override
    public boolean matches(String uri, HttpMethod requestMethod) {
        if (uri == null) {
            return false;
        }
        
        // HTTP Method 체크: method가 null이면 모든 메서드 허용
        if (method != null && method != requestMethod) {
            return false;
        }
        
        // 쿼리 스트링 제거 (ExactPathMatcher와 동일한 방식)
        String normalized = uri;
        int queryIdx = normalized.indexOf('?');
        if (queryIdx >= 0) {
            normalized = normalized.substring(0, queryIdx);
        }
        
        // 정확히 일치하는 경우
        if (normalized.equals(prefix)) {
            return true;
        }
        
        // 경로 경계 보장: prefix가 /로 끝나지 않으면 /를 추가하여 경계를 명확히 함
        // 예: /api -> /api/로 정규화하여 /apiary와 구분
        String normalizedPrefix = prefix.endsWith("/") ? prefix : prefix + "/";
        return normalized.startsWith(normalizedPrefix);
    }
    
    @Override
    public RateLimitRule getRule() {
        return rule;
    }
}

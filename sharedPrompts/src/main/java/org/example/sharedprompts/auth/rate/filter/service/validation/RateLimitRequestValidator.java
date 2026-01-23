package org.example.sharedprompts.auth.rate.filter.service.validation;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static org.example.sharedprompts.auth.rate.policy.RateLimitConstants.ApiPaths;

/**
 * Rate Limit 요청 검증 서비스
 * 
 * 요청이 Rate Limit을 적용할 대상인지 검증합니다.
 */
@Service
@RequiredArgsConstructor
public class RateLimitRequestValidator {

    /**
     * 요청이 Rate Limit을 적용할 대상인지 확인합니다.
     * 
     * @param uri 요청 URI
     * @return 적용 대상 여부
     */
    public boolean shouldApply(String uri) {
        return uri.startsWith(ApiPaths.API_PREFIX);
    }

    /**
     * HTTP Method를 파싱합니다.
     * 
     * @param request HttpServletRequest
     * @return Optional<HttpMethod> (파싱 실패 시 empty)
     */
    public Optional<HttpMethod> parseHttpMethod(HttpServletRequest request) {
        try {
            return Optional.of(HttpMethod.valueOf(request.getMethod()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}





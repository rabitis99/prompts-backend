package org.example.sharedprompts.domain.rate.ratelimitlog.service.file.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.auth.rate.RateLimiter;
import org.example.sharedprompts.auth.rate.policy.RateLimitRule;
import org.example.sharedprompts.domain.rate.ratelimitlog.service.file.RateLimitFileLogService;
import org.example.sharedprompts.global.util.HttpRequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import static org.example.sharedprompts.auth.rate.policy.RateLimitConstants.Logging;
import static org.example.sharedprompts.auth.rate.policy.RateLimitConstants.Response;

/**
 * Rate Limit 파일 로그 서비스 구현체
 */
@Service
@RequiredArgsConstructor
public class RateLimitFileLogServiceImpl implements RateLimitFileLogService {

    private static final Logger rateLimitLogger = LoggerFactory.getLogger(Logging.LOGGER_NAME);

    @Override
    public void logRateLimitExceeded(
            RateLimitRule rule,
            String key,
            RateLimiter.RateLimitResult result,
            HttpServletRequest request,
            Long userId
    ) {
        long retryAfter = result.getRetryAfter(Response.MIN_RETRY_AFTER_SECONDS);
        String uri = request.getRequestURI();
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        String clientIp = HttpRequestUtils.getClientIpAddress(request);

        if (userId != null) {
            // 사용자 기반 로그
            rateLimitLogger.warn(
                    Logging.LOG_FORMAT_USER,
                    rule.getName(), key, result.currentCount(), rule.getCapacity(),
                    retryAfter, userId, clientIp, uri, method
            );
        } else {
            // IP 기반 로그
            rateLimitLogger.warn(
                    Logging.LOG_FORMAT_IP,
                    rule.getName(), key, result.currentCount(), rule.getCapacity(),
                    retryAfter, clientIp, uri, method
            );
        }
    }

    @Override
    public void logRateLimitCheckFailed(String key, String error, Exception exception) {
        rateLimitLogger.error("Rate limit check failed, allowing request - key={}, error={}", 
                key, error, exception);
    }

    @Override
    public void logInvalidHttpMethod(String method) {
        rateLimitLogger.warn("Invalid HTTP method: {}", method);
    }
}






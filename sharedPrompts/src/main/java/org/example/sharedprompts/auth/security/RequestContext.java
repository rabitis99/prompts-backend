package org.example.sharedprompts.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Value;
import org.example.sharedprompts.global.util.HttpRequestUtils;

/**
 * HTTP 요청 컨텍스트 정보
 * 
 * IP 주소와 User-Agent를 함께 관리하는 값 객체
 */
@Value
public class RequestContext {
    String ip;
    String userAgent;
    
    /**
     * HttpServletRequest에서 RequestContext 생성
     * 
     * @param request HTTP 요청
     * @return RequestContext
     */
    public static RequestContext from(HttpServletRequest request) {
        return new RequestContext(
                HttpRequestUtils.getClientIpAddress(request),
                HttpRequestUtils.getUserAgent(request)
        );
    }
}


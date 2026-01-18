package org.example.sharedprompts.global.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * HTTP 요청 정보를 추출하는 유틸리티 클래스
 */
public class HttpRequestUtils {

    // 프록시 헤더 목록 (신뢰할 수 있는 경우에만 사용)
    // 주의: 클라이언트가 직접 조작할 수 있으므로 신뢰 경계 설정 필요
    private static final String[] PROXY_IP_HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED"
    };

    private static final String UNKNOWN_IP = "unknown";
    private static final int MAX_IP_LENGTH = 50;
    private static final int MAX_USER_AGENT_LENGTH = 1000;

    /**
     * 현재 요청의 IP 주소 추출
     * 프록시 환경을 고려하여 다양한 헤더를 확인합니다.
     * 
     * @return IP 주소 또는 "unknown"
     */
    public static String getClientIpAddress() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return UNKNOWN_IP;
        }

        HttpServletRequest request = attributes.getRequest();
        return getClientIpAddress(request);
    }

    /**
     * HttpServletRequest에서 IP 주소 추출
     * 
     * 보안 고려사항:
     * 1. request.getRemoteAddr()를 우선 사용 (가장 신뢰할 수 있음)
     * 2. 프록시 헤더는 신뢰할 수 있는 경우에만 사용
     *    - 프록시 헤더는 클라이언트가 조작 가능하므로 IP 스푸핑 취약점 존재
     *    - 운영 환경에서는 Spring Boot의 server.forward-headers-strategy 설정
     *      또는 리버스 프록시에서 신뢰되지 않은 헤더를 제거하도록 설정 필요
     * 
     * @param request HttpServletRequest
     * @return IP 주소 또는 "unknown"
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN_IP;
        }

        // 1) RemoteAddr를 우선 사용 (가장 신뢰할 수 있음)
        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr != null && !remoteAddr.isEmpty() && isValidIp(remoteAddr)) {
            return truncate(remoteAddr, MAX_IP_LENGTH);
        }

        // 2) 프록시 헤더는 신뢰할 수 있는 경우에만 사용
        // 주의: 프록시 헤더는 클라이언트가 조작 가능하므로 운영 환경에서 신뢰 경계 설정 필요
        // Spring Boot 설정 예시:
        // server.forward-headers-strategy: FRAMEWORK
        // 또는 리버스 프록시(Nginx, Apache)에서 신뢰되지 않은 헤더 제거
        for (String header : PROXY_IP_HEADERS) {
            String ip = request.getHeader(header);
            if (ip == null || ip.isEmpty() || UNKNOWN_IP.equalsIgnoreCase(ip)) {
                continue;
            }
            
            // X-Forwarded-For는 여러 IP가 콤마로 구분될 수 있음
            // 첫 번째 IP는 원래 클라이언트 IP (신뢰할 수 있는 프록시의 경우)
            if (ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            
            if (isValidIp(ip)) {
                // 프록시 헤더 사용 시 보안 로그 기록 권장 (운영 환경)
                // log.debug("프록시 헤더에서 IP 추출: header={}, ip={}, remoteAddr={}", header, ip, remoteAddr);
                return truncate(ip, MAX_IP_LENGTH);
            }
        }

        return UNKNOWN_IP;
    }

    /**
     * 현재 요청의 User-Agent 추출
     * 
     * @return User-Agent 또는 null
     */
    public static String getUserAgent() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        return getUserAgent(request);
    }

    /**
     * HttpServletRequest에서 User-Agent 추출
     * 
     * @param request HttpServletRequest
     * @return User-Agent 또는 null
     */
    public static String getUserAgent(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? truncate(userAgent, MAX_USER_AGENT_LENGTH) : null;
    }

    /**
     * IP 주소 유효성 검사 (간단한 형식 체크)
     * 
     * IPv4: 0.0.0.0 ~ 255.255.255.255
     * IPv6: 간단한 형식 체크 (완전한 검증은 복잡하므로 기본 형식만 확인)
     * localhost: localhost, 127.0.0.1, ::1
     */
    private static boolean isValidIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        
        // localhost 체크
        if ("localhost".equalsIgnoreCase(ip) || "127.0.0.1".equals(ip) || "::1".equals(ip)) {
            return true;
        }
        
        // IPv4 체크 (각 옥텟이 0-255 범위)
        if (ip.matches("^([0-9]{1,3}\\.){3}[0-9]{1,3}$")) {
            String[] parts = ip.split("\\.");
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            }
            return true;
        }
        
        // IPv6 기본 형식 체크 (간단한 검증)
        // 정확한 IPv6 검증은 복잡하므로 기본 형식만 확인
        if (ip.contains(":")) {
            // IPv6는 콜론으로 구분된 8개 그룹 (축약형 포함)
            return ip.matches("^([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}$") ||
                   ip.matches("^::([0-9a-fA-F]{0,4}:){0,6}[0-9a-fA-F]{0,4}$") ||
                   ip.matches("^([0-9a-fA-F]{0,4}:){1,7}::$");
        }
        
        return false;
    }

    /**
     * 문자열을 지정된 길이로 자름
     */
    private static String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }
}


package org.example.sharedprompts.auth.oauth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * OAuth2AuthorizationRequest 직렬화/역직렬화 전용 컴포넌트
 * 
 * <p>Spring Security의 SecurityJackson2Modules를 사용하여 안전하게 직렬화/역직렬화합니다.
 * Java 기본 직렬화 대신 JSON을 사용하여 역직렬화 공격을 방지합니다.
 * 
 * <p>책임:
 * <ul>
 *   <li>OAuth2AuthorizationRequest 객체를 Base64 URL 인코딩된 JSON 문자열로 직렬화</li>
 *   <li>Base64 URL 인코딩된 JSON 문자열을 OAuth2AuthorizationRequest 객체로 역직렬화</li>
 * </ul>
 */
@Slf4j
@Component
public class OAuth2AuthorizationRequestSerializer {

    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        // Spring Security의 SecurityJackson2Modules를 사용하여 안전한 직렬화 설정
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModules(SecurityJackson2Modules.getModules(getClass().getClassLoader()));
        this.objectMapper = mapper;
    }

    /**
     * OAuth2AuthorizationRequest를 JSON으로 직렬화합니다.
     * 
     * <p>Spring Security의 SecurityJackson2Modules를 사용하여 안전하게 직렬화합니다.
     * Java 기본 직렬화 대신 JSON을 사용하여 역직렬화 공격을 방지합니다.
     * 
     * @param object 직렬화할 객체 (OAuth2AuthorizationRequest)
     * @return Base64 URL 인코딩된 JSON 문자열
     * @throws ApiException 직렬화 실패 시
     */
    public String serialize(OAuth2AuthorizationRequest object) {
        try {
            String json = objectMapper.writeValueAsString(object);
            return Base64.getUrlEncoder().encodeToString(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("OAuth2AuthorizationRequest 직렬화 실패", e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, null, "OAuth2AuthorizationRequest 직렬화 실패", e);
        }
    }

    /**
     * Base64 URL 인코딩된 JSON 문자열을 OAuth2AuthorizationRequest로 역직렬화합니다.
     * 
     * <p>Spring Security의 SecurityJackson2Modules를 사용하여 안전하게 역직렬화합니다.
     * Java 기본 직렬화 대신 JSON을 사용하여 역직렬화 공격을 방지합니다.
     * 
     * @param cookie Base64 URL 인코딩된 JSON 문자열
     * @return 역직렬화된 객체 (실패 시 null)
     */
    public OAuth2AuthorizationRequest deserialize(String cookie) {
        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(cookie);
            String json = new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8);
            return objectMapper.readValue(json, OAuth2AuthorizationRequest.class);
        } catch (Exception e) {
            log.warn("OAuth2AuthorizationRequest 역직렬화 실패", e);
            return null; // 역직렬화 실패 시 null 반환 (호출부에서 처리)
        }
    }
}


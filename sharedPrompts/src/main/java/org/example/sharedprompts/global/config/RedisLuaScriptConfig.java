package org.example.sharedprompts.global.config;

import org.example.sharedprompts.global.Lua.LuaScripts;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

/**
 * Redis Lua 스크립트 설정
 * 
 * 책임:
 * - 모든 Redis Lua 스크립트를 Bean으로 등록
 * - 스크립트 초기화 및 타입 설정
 * 
 * 운영 원칙:
 * - @PostConstruct에서 외부 리소스(Redis) 접근 금지
 * - Lua 스크립트는 단순 객체 생성만 수행 (Redis 접근 없음)
 * - 스크립트 실행은 각 Repository/Store에서 수행
 * 
 * 장애 시나리오:
 * - 스크립트 객체 생성 실패: 애플리케이션 기동 실패 (즉시 감지)
 * - 스크립트 실행 실패: Circuit Breaker 및 Fallback 처리 (런타임)
 */
@Configuration
public class RedisLuaScriptConfig {

    /**
     * INCREMENT_WITH_TTL 스크립트
     * 
     * 사용처:
     * - TokenVersionStoreImpl: tokenVersion 증가
     * - BaseCountServiceImpl: 카운트 증가
     * 
     * 기능: INCR + EXPIRE를 원자적으로 수행
     */
    @Bean
    public DefaultRedisScript<Long> incrementWithTtlScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(LuaScripts.INCREMENT_WITH_TTL);
        script.setResultType(Long.class);
        return script;
    }

    /**
     * SAFE_DECREMENT_WITH_TTL 스크립트
     * 
     * 사용처:
     * - BaseCountServiceImpl: 카운트 감소
     * 
     * 기능: 안전한 DECR (0 이하로 감소 방지) + EXPIRE를 원자적으로 수행
     */
    @Bean
    public DefaultRedisScript<Long> safeDecrementWithTtlScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(LuaScripts.SAFE_DECREMENT_WITH_TTL);
        script.setResultType(Long.class);
        return script;
    }

    /**
     * GET_AND_DELETE_REFRESH_TOKEN 스크립트
     * 
     * 사용처:
     * - RefreshTokenStoreImpl: Refresh Token 조회 및 삭제 (1회용 보장)
     * 
     * 기능: Refresh Token 조회 + 삭제를 원자적으로 수행
     * 반환값: List<String> [userId, ip, userAgent, remainingTtlMillis]
     */
    @Bean
    public DefaultRedisScript<List<String>> getAndDeleteRefreshTokenScript() {
        DefaultRedisScript<List<String>> script = new DefaultRedisScript<>();
        script.setScriptText(LuaScripts.GET_AND_DELETE_REFRESH_TOKEN);
        // Spring Data Redis는 런타임에 제네릭 타입 정보를 잃어버리므로 raw type을 사용
        @SuppressWarnings("unchecked")
        Class<List<String>> resultType = (Class<List<String>>) (Class<?>) List.class;
        script.setResultType(resultType);
        return script;
    }

    /**
     * DELETE_REFRESH_TOKEN 스크립트
     * 
     * 사용처:
     * - RefreshTokenStoreImpl: Refresh Token 삭제
     * 
     * 기능: Refresh Token, 메타데이터, 사용자별 Set에서 원자적으로 삭제
     */
    @Bean
    public DefaultRedisScript<Long> deleteRefreshTokenScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(LuaScripts.DELETE_REFRESH_TOKEN);
        script.setResultType(Long.class);
        return script;
    }

    /**
     * DELETE_ALL_REFRESH_TOKENS_BY_USER 스크립트
     * 
     * 사용처:
     * - RefreshTokenStoreImpl: 사용자별 모든 Refresh Token 삭제
     * 
     * 기능: 사용자별 모든 Refresh Token을 원자적으로 삭제
     */
    @Bean
    public DefaultRedisScript<Long> deleteAllRefreshTokensByUserScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(LuaScripts.DELETE_ALL_REFRESH_TOKENS_BY_USER);
        script.setResultType(Long.class);
        return script;
    }

    /**
     * GET_AND_DELETE_TEMP_TOKEN 스크립트
     * 
     * 사용처:
     * - TokenRedisServiceImpl: OAuth2 임시 토큰 조회 및 삭제
     * 
     * 기능: OAuth2 임시 토큰 조회 + 삭제를 원자적으로 수행
     * 반환값: List<String> [field1, value1, field2, value2, ...]
     */
    @Bean
    public DefaultRedisScript<List<String>> getAndDeleteTempTokenScript() {
        DefaultRedisScript<List<String>> script = new DefaultRedisScript<>();
        script.setScriptText(LuaScripts.GET_AND_DELETE_TEMP_TOKEN);
        // Spring Data Redis는 런타임에 제네릭 타입 정보를 잃어버리므로 raw type을 사용
        @SuppressWarnings("unchecked")
        Class<List<String>> resultType = (Class<List<String>>) (Class<?>) List.class;
        script.setResultType(resultType);
        return script;
    }
}


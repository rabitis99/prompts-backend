package org.example.sharedprompts.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 캐시 설정
 * - 읽지 않은 알림 개수 등에 대한 캐싱 지원
 * - 통계 데이터 캐싱 지원
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final String NOTIFICATION_CACHE_NAME = "notification";
    private static final String STATISTICS_CACHE_NAME = "statistics";

    @Value("${notification.cache.unread-count-ttl:60}")
    private long unreadCountTtl;

    @Value("${statistics.cache.ttl:300}")
    private long statisticsTtl;

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        // 도메인 DTO, java.time, 컬렉션 인터페이스만 허용하는 보안 강화된 타입 검증자
        BasicPolymorphicTypeValidator validator = BasicPolymorphicTypeValidator.builder()
                // 통계 관련 DTO 패키지만 허용 (StatisticsResponseDto, UserStatisticsResponseDto 등)
                .allowIfSubType("org.example.sharedprompts.dto.statistics.response")
                // 알림 관련 DTO 패키지만 허용 (NotificationSummaryDto)
                .allowIfSubType("org.example.sharedprompts.dto.notification.response")
                // java.time 패키지만 허용 (날짜/시간 타입)
                .allowIfSubType("java.time")
                // 컬렉션 인터페이스만 허용 (구현체는 허용하지 않음)
                .allowIfBaseType(List.class)
                .allowIfBaseType(Map.class)
                .allowIfBaseType(Set.class)
                .build();

        ObjectMapper cacheObjectMapper = objectMapper.copy();
        // 다형성 타입 정보 활성화 (Redis 직렬화 시 필수)
        // NON_FINAL: final이 아닌 타입에만 타입 정보 포함
        // - List<DailyNewUsersTrendDto>, Map 등 컬렉션과 제네릭 타입 역직렬화에 필요
        // - 중첩 DTO (StatisticsResponseDto 내부의 UserStatisticsResponseDto 등) 올바른 역직렬화 보장
        // - BasicPolymorphicTypeValidator와 함께 사용하여 허용된 타입만 역직렬화
        cacheObjectMapper.activateDefaultTyping(
                validator,
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(cacheObjectMapper);

        // 기본 캐시 설정 (알림용)
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(unreadCountTtl))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();

        // 통계 캐시 설정
        RedisCacheConfiguration statisticsConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(statisticsTtl))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();

        // 캐시별 설정 맵
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(NOTIFICATION_CACHE_NAME, defaultConfig);
        cacheConfigurations.put(STATISTICS_CACHE_NAME, statisticsConfig);

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}


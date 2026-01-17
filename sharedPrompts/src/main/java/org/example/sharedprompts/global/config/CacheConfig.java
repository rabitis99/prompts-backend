package org.example.sharedprompts.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
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
import java.util.Map;

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
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory,
                                      @Qualifier("redisObjectMapper") ObjectMapper redisObjectMapper) {
        // BasicPolymorphicTypeValidator를 사용한 안전한 직렬화기 사용
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper);
        
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


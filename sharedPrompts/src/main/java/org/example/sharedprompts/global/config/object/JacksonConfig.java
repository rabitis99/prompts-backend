package org.example.sharedprompts.global.config.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    /**
     * Spring 관리 ObjectMapper 빈 생성
     * 설정 드리프트를 방지하기 위해 일관된 설정을 적용합니다.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        JavaTimeModule javaTimeModule = new JavaTimeModule();
        // LocalDateTime: 앱 전역에서 UTC로 다루므로(JpaAuditingConfig, Hibernate time_zone) 직렬화 시 ISO-8601+Z로 출력해 클라이언트가 UTC로 해석하도록 함
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeUtcSerializer());
        javaTimeModule.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        // Instant: ISO-8601 형식으로 직렬화 (PayPal 등 외부 API UTC 타임스탬프 처리용)
        javaTimeModule.addSerializer(Instant.class, InstantSerializer.INSTANCE);
        javaTimeModule.addDeserializer(Instant.class, InstantDeserializer.INSTANT);
        mapper.registerModule(javaTimeModule);

        // 기타 설정
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        return mapper;
    }

    /**
     * Redis 직렬화용 안전한 ObjectMapper
     * - BasicPolymorphicTypeValidator를 사용하여 신뢰할 수 있는 패키지만 허용
     * - LaissezFaireSubTypeValidator의 취약점 방지 (gadget 기반 역직렬화 공격 차단)
     */
    @Bean("redisObjectMapper")
    public ObjectMapper redisObjectMapper() {
        // 신뢰할 수 있는 패키지만 허용하는 타입 검증기 생성
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("org.example.sharedprompts")
                .allowIfSubType("java.util")
                .allowIfSubType("java.lang")
                .allowIfSubType("java.time")
                .build();

        ObjectMapper mapper = new ObjectMapper();

        // 다형성 타입 정보 활성화 (Redis 직렬화에 필요)
        mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);

        // JavaTime 모듈 등록
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        javaTimeModule.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        // Instant: ISO-8601 형식으로 직렬화 (PayPal 등 외부 API UTC 타임스탬프 처리용)
        javaTimeModule.addSerializer(Instant.class, InstantSerializer.INSTANCE);
        javaTimeModule.addDeserializer(Instant.class, InstantDeserializer.INSTANT);
        mapper.registerModule(javaTimeModule);

        // 기본 설정 (objectMapper()와 동일한 네이밍 전략 적용)
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // INDENT_OUTPUT은 Redis 저장 공간을 위해 비활성화 유지

        return mapper;
    }

    /**
     * LocalDateTime을 UTC라고 간주하고 ISO-8601 형식(접미사 Z)으로 직렬화합니다.
     * JpaAuditingConfig·Hibernate time_zone으로 저장된 값과 일치시켜 API 응답의 시간대 일관성을 보장합니다.
     */
    private static final class LocalDateTimeUtcSerializer extends JsonSerializer<LocalDateTime> {
        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == null) {
                gen.writeNull();
                return;
            }
            Instant instant = value.atOffset(ZoneOffset.UTC).toInstant();
            gen.writeString(DateTimeFormatter.ISO_INSTANT.format(instant));
        }
    }
}
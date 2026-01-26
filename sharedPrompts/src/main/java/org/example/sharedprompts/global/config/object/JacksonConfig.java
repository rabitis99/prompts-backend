package org.example.sharedprompts.global.config.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    /**
     * Creates a centrally configured ObjectMapper managed by Spring for application use.
     *
     * Configures LocalDateTime formatting with pattern "yyyy-MM-dd HH:mm:ss", excludes null values,
     * uses snake_case property naming, disables date timestamps, and enables pretty printing.
     *
     * @return the configured ObjectMapper instance with application-wide serialization settings
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        javaTimeModule.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        mapper.registerModule(javaTimeModule);

        // 기타 설정
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        return mapper;
    }

    /**
     * Create an ObjectMapper configured for safe Redis serialization with restricted polymorphic typing.
     *
     * <p>Configures a BasicPolymorphicTypeValidator that only allows subtypes from
     * org.example.sharedprompts, java.util, java.lang, and java.time to mitigate gadget-based
     * deserialization attacks. Activates default typing for non-final types, registers a
     * JavaTimeModule using the pattern "yyyy-MM-dd HH:mm:ss" for LocalDateTime, excludes null
     * properties from serialization, uses snake_case property naming, and disables writing dates
     * as timestamps. Pretty printing is intentionally not enabled.</p>
     *
     * @return an ObjectMapper configured for Redis serialization with safe polymorphic typing and Java time handling
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
        mapper.registerModule(javaTimeModule);

        // 기본 설정 (objectMapper()와 동일한 네이밍 전략 적용)
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // INDENT_OUTPUT은 Redis 저장 공간을 위해 비활성화 유지

        return mapper;
    }
}





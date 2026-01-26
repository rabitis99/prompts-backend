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
import org.example.sharedprompts.auth.jwt.util.JwtErrorResponseWriter;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    private ObjectMapper objectMapper;

    /**
     * Spring 관리 ObjectMapper 빈 생성
     * 설정 드리프트를 방지하기 위해 일관된 설정을 적용합니다.
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
     * ObjectMapper 빈 주입
     * @Bean 메서드로 생성된 ObjectMapper를 주입받습니다.
     */
    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * JwtErrorResponseWriter에 Spring 관리 ObjectMapper 주입
     * @PostConstruct를 사용하여 빈 초기화 완료 후 주입을 보장합니다.
     * 이 방식은 빈 초기화 순서에 의존하지 않고, 테스트 시 모킹이 용이합니다.
     * 
     * 참고: AuditLogPublisher는 생성자 주입을 사용하므로 여기서 별도 설정이 필요 없습니다.
     */
    @PostConstruct
    public void initializeJwtErrorResponseWriter() {
        JwtErrorResponseWriter.setObjectMapper(objectMapper);
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
        mapper.registerModule(javaTimeModule);

        // 기본 설정 (objectMapper()와 동일한 네이밍 전략 적용)
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // INDENT_OUTPUT은 Redis 저장 공간을 위해 비활성화 유지

        return mapper;
    }
}

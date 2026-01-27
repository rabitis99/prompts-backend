package org.example.sharedprompts.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * 애플리케이션 시작 시 환경 변수 및 프로필 검증
 * 
 * <p>프로덕션 환경에서 필수 환경 변수가 누락되거나 잘못된 프로필이 설정된 경우
 * 애플리케이션 시작을 중단합니다.
 * 
 * <p>EnvironmentPostProcessor를 사용하여 빈 생성 전에 검증을 수행합니다.
 * 이를 통해 리소스(데이터베이스 연결, 포트 바인딩 등)가 할당되기 전에 
 * 필수 환경 변수를 검증할 수 있습니다.
 * 
 * <p>이 클래스는 META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor 파일에
 * 등록되어야 합니다.
 */
@Slf4j
public class EnvironmentValidator implements EnvironmentPostProcessor {

    private static final String PROD_PROFILE = "prod";
    private static final String DEV_PROFILE = "dev";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String[] activeProfiles = environment.getActiveProfiles();
        
        log.info("Active profiles: {}", String.join(", ", activeProfiles.length > 0 ? activeProfiles : new String[]{"default"}));
        
        // 프로필 검증
        validateProfile(environment, activeProfiles);
        
        // 환경 변수 검증
        validateEnvironmentVariables(environment, activeProfiles);
        
        // 설정 검증 (JPA DDL, 로깅 레벨 등)
        validateConfiguration(environment, activeProfiles);
        
        log.info("✅ 환경 변수 및 프로필 검증 완료");
    }

    /**
     * 프로필 검증
     * 프로덕션 환경에서는 반드시 'prod' 프로필이 활성화되어야 합니다.
     */
    private void validateProfile(Environment env, String[] activeProfiles) {
        boolean isProdProfile = containsProfile(activeProfiles, PROD_PROFILE);
        boolean isDevProfile = containsProfile(activeProfiles, DEV_PROFILE);
        
        // 프로덕션 환경 감지 (환경 변수 또는 시스템 속성으로 판단)
        String deploymentEnv = env.getProperty("DEPLOYMENT_ENV", String.class);
        boolean isProductionEnvironment = "production".equalsIgnoreCase(deploymentEnv) 
                || "prod".equalsIgnoreCase(deploymentEnv)
                || System.getProperty("deployment.env", "").equalsIgnoreCase("production");
        
        if (isProductionEnvironment && !isProdProfile) {
            String errorMessage = String.format(
                "❌ 프로덕션 환경에서는 반드시 'prod' 프로필이 활성화되어야 합니다. " +
                "현재 활성 프로필: %s. " +
                "환경 변수 SPRING_PROFILES_ACTIVE=prod를 설정하세요.",
                String.join(", ", activeProfiles.length > 0 ? activeProfiles : new String[]{"default"})
            );
            log.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }
        
        // 개발 환경에서 prod 프로필이 활성화된 경우 경고
        if (isDevProfile && isProdProfile) {
            log.warn("⚠️ 개발 프로필과 프로덕션 프로필이 동시에 활성화되었습니다. 개발 환경에서는 'dev' 프로필만 사용하는 것을 권장합니다.");
        }
    }

    /**
     * 환경 변수 검증
     * 필수 환경 변수가 누락되었는지 확인합니다.
     */
    private void validateEnvironmentVariables(Environment env, String[] activeProfiles) {
        boolean isProdProfile = containsProfile(activeProfiles, PROD_PROFILE);
        List<String> missingVariables = new ArrayList<>();
        
        // 공통 필수 환경 변수
        List<String> requiredVariables = new ArrayList<>(List.of(
            "JWT_SECRET",
            "SPRING_DATASOURCE_URL",
            "SPRING_DATASOURCE_USERNAME",
            "SPRING_DATASOURCE_PASSWORD",
            "CORS_ALLOWED_ORIGINS",
            "OAUTH2_REDIRECT_FRONT_URL",
            "OAUTH2_FAILURE_REDIRECT_URL",
            "OAUTH2_SALT"
        ));
        
        // OAuth2 클라이언트 정보
        requiredVariables.addAll(List.of(
            "GOOGLE_CLIENT_ID",
            "GOOGLE_CLIENT_SECRET",
            "NAVER_CLIENT_ID",
            "NAVER_CLIENT_SECRET",
            "KAKAO_CLIENT_ID",
            "KAKAO_CLIENT_SECRET"
        ));
        
        // 프로덕션 환경에서만 필수인 변수들
        if (isProdProfile) {
            requiredVariables.addAll(List.of(
                "GOOGLE_GEMINI_API_KEY",
                "SPRING_AI_OPENAI_API_KEY"
            ));
        }
        
        // 환경 변수 검증
        for (String varName : requiredVariables) {
            String value = env.getProperty(varName);
            if (value == null || value.trim().isEmpty()) {
                missingVariables.add(varName);
            }
        }
        
        // 더미 API 키 검증 (프로덕션 환경)
        if (isProdProfile) {
            String openaiApiKey = env.getProperty("SPRING_AI_OPENAI_API_KEY");
            if (openaiApiKey != null && openaiApiKey.equals("dummy-openai-api-key")) {
                log.error("❌ 프로덕션 환경에서 더미 OpenAI API 키가 사용되고 있습니다. 실제 API 키를 설정하세요.");
                throw new IllegalStateException("프로덕션 환경에서는 실제 OpenAI API 키가 필요합니다.");
            }
        }
        
        // 누락된 환경 변수 보고
        if (!missingVariables.isEmpty()) {
            String errorMessage = String.format(
                "❌ 필수 환경 변수가 누락되었습니다: %s\n" +
                "배포 전 체크리스트를 확인하고 모든 필수 환경 변수를 설정하세요.",
                String.join(", ", missingVariables)
            );
            log.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }
        
        // 프로덕션 환경에서 localhost 사용 경고
        if (isProdProfile) {
            String redisHost = env.getProperty("REDIS_HOST", "localhost");
            String rabbitmqHost = env.getProperty("RABBITMQ_HOST", "localhost");
            
            if ("localhost".equals(redisHost) || "127.0.0.1".equals(redisHost)) {
                log.warn("⚠️ 프로덕션 환경에서 Redis 호스트가 localhost로 설정되어 있습니다. 실제 Redis 서버 주소를 설정하세요.");
            }
            
            if ("localhost".equals(rabbitmqHost) || "127.0.0.1".equals(rabbitmqHost)) {
                log.warn("⚠️ 프로덕션 환경에서 RabbitMQ 호스트가 localhost로 설정되어 있습니다. 실제 RabbitMQ 서버 주소를 설정하세요.");
            }
            
            // RabbitMQ 기본 자격 증명 경고
            String rabbitmqUsername = env.getProperty("RABBITMQ_USERNAME", "guest");
            String rabbitmqPassword = env.getProperty("RABBITMQ_PASSWORD", "guest");
            
            if ("guest".equals(rabbitmqUsername) && "guest".equals(rabbitmqPassword)) {
                log.warn("⚠️ 프로덕션 환경에서 RabbitMQ 기본 자격 증명(guest/guest)이 사용되고 있습니다. 강력한 자격 증명으로 변경하세요.");
            }
        }
    }

    /**
     * 설정 검증
     * 프로덕션 환경에서 위험한 설정이 사용되고 있는지 확인합니다.
     */
    private void validateConfiguration(Environment env, String[] activeProfiles) {
        boolean isProdProfile = containsProfile(activeProfiles, PROD_PROFILE);
        
        if (!isProdProfile) {
            return; // 프로덕션 환경이 아니면 검증 스킵
        }
        
        // JPA DDL Auto 설정 검증 (프로덕션에서 update 사용 시 위험)
        String ddlAuto = env.getProperty("spring.jpa.hibernate.ddl-auto", 
                env.getProperty("JPA_DDL_AUTO", "none"));
        if ("update".equalsIgnoreCase(ddlAuto) || "create".equalsIgnoreCase(ddlAuto) 
                || "create-drop".equalsIgnoreCase(ddlAuto)) {
            String errorMessage = String.format(
                "❌ 프로덕션 환경에서 위험한 JPA DDL 설정이 감지되었습니다: %s. " +
                "프로덕션 환경에서는 반드시 'none'으로 설정해야 합니다. " +
                "스키마 변경은 마이그레이션 도구(Flyway, Liquibase)를 사용하세요.",
                ddlAuto
            );
            log.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }
        
        // 로깅 레벨 검증 (프로덕션에서 DEBUG 사용 시 경고)
        String rootLogLevel = env.getProperty("logging.level.root", "INFO");
        if ("DEBUG".equalsIgnoreCase(rootLogLevel) || "TRACE".equalsIgnoreCase(rootLogLevel)) {
            log.warn("⚠️ 프로덕션 환경에서 DEBUG/TRACE 로깅 레벨이 설정되어 있습니다. " +
                    "성능 및 보안을 위해 INFO 레벨 이상을 권장합니다.");
        }
        
        // 커넥션 풀 크기 경고 (기본값 사용 시)
        String maxPoolSize = env.getProperty("HIKARI_MAXIMUM_POOL_SIZE");
        if (maxPoolSize == null || maxPoolSize.trim().isEmpty()) {
            log.warn("⚠️ 프로덕션 환경에서 HikariCP 커넥션 풀 크기가 기본값으로 설정되어 있습니다. " +
                    "실제 트래픽에 맞게 HIKARI_MAXIMUM_POOL_SIZE를 조정하는 것을 권장합니다.");
        }
        
        // OAuth2 리다이렉트 URL 형식 검증
        String oauth2RedirectUrl = env.getProperty("OAUTH2_REDIRECT_FRONT_URL");
        if (oauth2RedirectUrl != null && !oauth2RedirectUrl.trim().isEmpty()) {
            if (!oauth2RedirectUrl.startsWith("http://") && !oauth2RedirectUrl.startsWith("https://")) {
                log.warn("⚠️ OAuth2 리다이렉트 URL이 올바른 형식이 아닐 수 있습니다: {}. " +
                        "실제 OAuth 제공자에 등록된 URL과 일치하는지 확인하세요.", oauth2RedirectUrl);
            }
        }
    }

    private boolean containsProfile(String[] profiles, String profile) {
        for (String p : profiles) {
            if (p.equals(profile)) {
                return true;
            }
        }
        return false;
    }
}


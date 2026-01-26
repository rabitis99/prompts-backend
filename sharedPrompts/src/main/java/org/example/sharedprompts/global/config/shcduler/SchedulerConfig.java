package org.example.sharedprompts.global.config.shcduler;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;
import java.time.Clock;

@Slf4j
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "15m")
public class SchedulerConfig {

    /**
     * Redis LockProvider (Primary)
     * Fallback이 비활성화된 경우 직접 사용되고, 활성화된 경우 FallbackLockProvider의 primary로 사용됨
     */
    @Bean
    public RedisLockProvider redisLockProvider(RedisConnectionFactory redisConnectionFactory) {
        return new RedisLockProvider(redisConnectionFactory);
    }

    /**
     * DB LockProvider (Fallback)
     * Redis 장애 시 사용되는 백업 LockProvider
     */
    @Bean
    @ConditionalOnProperty(name = "shedlock.fallback.enabled", havingValue = "true")
    public LockProvider dbLockProvider(
            DataSource dataSource,
            @Value("${shedlock.table.auto-create:false}") boolean autoCreate) {
        log.info("DB LockProvider (fallback) initialized");
        
        // 테이블 확인 및 필요시 생성
        ensureShedLockTable(dataSource, autoCreate);
        
        return new JdbcTemplateLockProvider(dataSource);
    }
    
    /**
     * ShedLock 테이블 존재 여부 확인 및 필요시 생성
     */
    private void ensureShedLockTable(DataSource dataSource, boolean autoCreate) {
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            String sql = "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = DATABASE() AND table_name = 'shedlock'";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            
            if (count == null || count == 0) {
                if (autoCreate) {
                    log.info("ShedLock table not found, creating table...");
                    createShedLockTable(jdbcTemplate);
                } else {
                    log.warn("ShedLock table does not exist. " +
                            "Please create it manually using: src/main/resources/db/migration/shedlock.sql " +
                            "Or enable auto-create by setting: shedlock.table.auto-create=true");
                }
            } else {
                log.debug("ShedLock table exists");
            }
        } catch (Exception e) {
            log.warn("Failed to check/create ShedLock table", e);
        }
    }
    
    /**
     * ShedLock 테이블 생성
     */
    private void createShedLockTable(JdbcTemplate jdbcTemplate) {
        try {
            String createTableSql = "CREATE TABLE IF NOT EXISTS shedlock (" +
                    "name VARCHAR(64) NOT NULL COMMENT '스케줄러 이름 (Primary Key)', " +
                    "lock_until TIMESTAMP(3) NOT NULL COMMENT '락 만료 시간', " +
                    "locked_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '락 획득 시간', " +
                    "locked_by VARCHAR(255) NOT NULL COMMENT '락을 획득한 인스턴스 식별자 (hostname:pid)', " +
                    "PRIMARY KEY (name)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci " +
                    "COMMENT='ShedLock 분산 락 테이블 - Redis 장애 시 DB 기반 LockProvider 사용'";
            
            jdbcTemplate.execute(createTableSql);
            
            // 인덱스 생성 (이미 존재하면 에러가 발생할 수 있으므로 try-catch)
            try {
                jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_shedlock_lock_until ON shedlock(lock_until)");
            } catch (Exception e) {
                log.debug("Index may already exist: {}", e.getMessage());
            }
            
            log.info("ShedLock table created successfully");
        } catch (Exception e) {
            log.error("Failed to create ShedLock table", e);
            throw new RuntimeException("Failed to create ShedLock table", e);
        }
    }

    /**
     * 메인 LockProvider Bean (Fallback 활성화 시)
     * 
     * Redis 장애 시 자동으로 DB 기반 LockProvider로 전환하는 FallbackLockProvider 사용
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "shedlock.fallback.enabled", havingValue = "true")
    public LockProvider fallbackLockProvider(
            RedisLockProvider redisLockProvider,
            @Qualifier("dbLockProvider") LockProvider dbLockProvider) {
        log.info("FallbackLockProvider initialized with Redis primary and DB fallback");
        return new FallbackLockProvider(redisLockProvider, dbLockProvider);
    }

    /**
     * 메인 LockProvider Bean (Fallback 비활성화 시)
     * 
     * Fallback이 비활성화된 경우 Redis LockProvider만 사용
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "shedlock.fallback.enabled", havingValue = "false", matchIfMissing = true)
    public LockProvider lockProvider(RedisLockProvider redisLockProvider) {
        log.info("Using Redis LockProvider only (fallback disabled)");
        return redisLockProvider;
    }

    /**
     * UTC 기준 Clock Bean
     * 
     * 스케줄러에서 타임존을 명시적으로 제어하기 위해 UTC Clock을 제공합니다.
     * 클라우드 환경에서 컨테이너가 UTC이지만 애플리케이션이 다른 타임존을 기대할 경우,
     * 저장된 createdAt과 일치하도록 UTC를 사용합니다.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
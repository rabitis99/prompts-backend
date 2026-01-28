package org.example.sharedprompts.global.config.scheduler;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.example.sharedprompts.global.config.scheduler.fallback.FallbackLockProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
     * Redis LockProvider
     */
    @Bean
    public RedisLockProvider redisLockProvider(RedisConnectionFactory redisConnectionFactory) {
        return new RedisLockProvider(redisConnectionFactory);
    }

    /**
     * DB LockProvider (Fallback 용)
     */
    @Bean
    @ConditionalOnProperty(name = "shedlock.fallback.enabled", havingValue = "true")
    public LockProvider dbLockProvider(
            DataSource dataSource,
            @Value("${shedlock.table.auto-create:false}") boolean autoCreate
    ) {
        log.info("DB LockProvider (fallback) initialized");
        ensureShedLockTable(dataSource, autoCreate);
        return new JdbcTemplateLockProvider(dataSource);
    }

    /**
     * 메인 LockProvider (Fallback 활성화 시)
     */
    @Bean
    @ConditionalOnProperty(name = "shedlock.fallback.enabled", havingValue = "true")
    public LockProvider fallbackLockProvider(
            RedisLockProvider redisLockProvider,
            @Qualifier("dbLockProvider") LockProvider dbLockProvider
    ) {
        log.info("Using FallbackLockProvider (Redis → DB)");
        return new FallbackLockProvider(redisLockProvider, dbLockProvider);
    }

    /**
     * 메인 LockProvider (Fallback 비활성화 시)
     */
    @Bean
    @ConditionalOnProperty(
            name = "shedlock.fallback.enabled",
            havingValue = "false",
            matchIfMissing = true
    )
    public LockProvider lockProvider(RedisLockProvider redisLockProvider) {
        log.info("Using Redis LockProvider only (fallback disabled)");
        return redisLockProvider;
    }

    /**
     * ShedLock 테이블 존재 여부 확인 및 필요 시 생성
     */
    private void ensureShedLockTable(DataSource dataSource, boolean autoCreate) {
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            String sql =
                    "SELECT COUNT(*) FROM information_schema.tables " +
                            "WHERE table_schema = DATABASE() AND table_name = 'shedlock'";

            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);

            if (count == null || count == 0) {
                if (autoCreate) {
                    log.info("ShedLock table not found, creating table...");
                    createShedLockTable(jdbcTemplate);
                } else {
                    log.warn(
                            "ShedLock table does not exist. " +
                                    "Create it manually or enable shedlock.table.auto-create=true"
                    );
                }
            } else {
                log.debug("ShedLock table exists");
            }
        } catch (Exception e) {
            log.warn("Failed to check/create ShedLock table", e);
            if (autoCreate) {
                throw new IllegalStateException(
                        "ShedLock table init failed while auto-create is enabled", e
                );
            }
        }
    }

    /**
     * ShedLock 테이블 생성
     */
    private void createShedLockTable(JdbcTemplate jdbcTemplate) {
        String createTableSql =
                "CREATE TABLE IF NOT EXISTS shedlock (" +
                        " name VARCHAR(64) NOT NULL COMMENT '스케줄러 이름 (PK)'," +
                        " lock_until TIMESTAMP(3) NOT NULL COMMENT '락 만료 시간'," +
                        " locked_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '락 획득 시간'," +
                        " locked_by VARCHAR(255) NOT NULL COMMENT '락 획득 인스턴스'," +
                        " PRIMARY KEY (name)," +
                        " INDEX idx_shedlock_lock_until (lock_until)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 " +
                        "COLLATE=utf8mb4_unicode_ci " +
                        "COMMENT='ShedLock fallback DB table'";

        jdbcTemplate.execute(createTableSql);
        log.info("ShedLock table created successfully");
    }

    /**
     * UTC Clock
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}

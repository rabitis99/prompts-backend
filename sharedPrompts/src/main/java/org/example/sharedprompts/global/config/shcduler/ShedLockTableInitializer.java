package org.example.sharedprompts.global.config.shcduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;

/**
 * ShedLock 테이블 자동 초기화
 * 
 * <p>애플리케이션 시작 시 shedlock 테이블이 없으면 자동으로 생성합니다.
 * 
 * <p>활성화 조건:
 * <ul>
 *   <li>shedlock.fallback.enabled=true (Fallback 기능 활성화)</li>
 *   <li>shedlock.table.auto-create=true (자동 생성 활성화, 기본값: false)</li>
 * </ul>
 * 
 * <p>주의사항:
 * <ul>
 *   <li>프로덕션 환경에서는 수동으로 테이블을 생성하는 것을 권장</li>
 *   <li>이 기능은 개발 환경에서만 사용하는 것을 권장</li>
 * </ul>
 */
@Slf4j
@Component
@ConditionalOnProperty(
    name = {"shedlock.fallback.enabled", "shedlock.table.auto-create"},
    havingValue = "true",
    matchIfMissing = false
)
public class ShedLockTableInitializer {

    private final JdbcTemplate jdbcTemplate;

    public ShedLockTableInitializer(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeShedLockTable() {
        try {
            // 테이블 존재 여부 확인
            if (tableExists()) {
                log.debug("ShedLock table already exists, skipping initialization");
                return;
            }

            log.info("ShedLock table not found, creating table...");
            
            // SQL 스크립트 읽기
            ClassPathResource resource = new ClassPathResource("db/migration/shedlock.sql");
            String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            
            // SQL 실행 (주석 제거 및 실행)
            String[] statements = sql.split(";");
            for (String statement : statements) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                    try {
                        jdbcTemplate.execute(trimmed);
                        log.debug("Executed SQL statement: {}", trimmed.substring(0, Math.min(50, trimmed.length())));
                    } catch (Exception e) {
                        log.warn("Failed to execute SQL statement: {}", trimmed.substring(0, Math.min(50, trimmed.length())), e);
                    }
                }
            }
            
            log.info("ShedLock table created successfully");
        } catch (Exception e) {
            log.error("Failed to initialize ShedLock table", e);
            // 테이블 생성 실패해도 애플리케이션은 계속 실행
            // Fallback 기능은 테이블이 없으면 사용할 수 없지만, Redis가 정상이면 문제없음
        }
    }

    /**
     * shedlock 테이블 존재 여부 확인
     */
    private boolean tableExists() {
        try {
            String sql = "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = DATABASE() AND table_name = 'shedlock'";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            log.debug("Error checking if shedlock table exists", e);
            return false;
        }
    }
}


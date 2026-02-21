package org.example.sharedprompts.module.domain.production.application.outbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Production Job Outbox 테이블 자동 초기화.
 *
 * <p>애플리케이션 시작 시 production_job_outbox 테이블이 없으면 outbox.sql을 실행하여 생성합니다.
 *
 * <p>활성화: production.outbox.table.auto-create=true
 *
 * <p>프로덕션에서는 Flyway 등으로 스키마를 관리하는 경우 이 초기화를 비활성화하고
 * db/migration/outbox.sql을 수동 또는 마이그레이션 도구로 적용하는 것을 권장합니다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "production.outbox.table.auto-create", havingValue = "true", matchIfMissing = false)
public class OutboxTableInitializer {

    private static final String OUTBOX_TABLE_NAME = "production_job_outbox";

    private final JdbcTemplate jdbcTemplate;

    public OutboxTableInitializer(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeOutboxTable() {
        try {
            if (tableExists()) {
                log.debug("Outbox table {} already exists, skipping initialization", OUTBOX_TABLE_NAME);
                return;
            }
            log.info("Outbox table not found, creating {}...", OUTBOX_TABLE_NAME);
            ClassPathResource resource = new ClassPathResource("db/migration/outbox.sql");
            try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
                ScriptUtils.executeSqlScript(connection, resource);
            }
            log.info("Outbox table created successfully");
        } catch (Exception e) {
            log.error("Failed to initialize outbox table", e);
        }
    }

    private boolean tableExists() {
        try {
            String sql = "SELECT COUNT(*) FROM information_schema.tables " +
                    "WHERE table_schema = DATABASE() AND table_name = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, OUTBOX_TABLE_NAME);
            return count != null && count > 0;
        } catch (Exception e) {
            log.debug("Error checking if outbox table exists", e);
            return false;
        }
    }
}

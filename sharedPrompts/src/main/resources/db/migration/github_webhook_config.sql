-- ============================================
-- GitHub Webhook 설정 테이블 (사용자별 Webhook → promptId 매핑)
-- ============================================
-- 수동 또는 마이그레이션 도구로 적용. Flyway 미사용 시 배포 체크리스트에 포함.
-- ============================================

CREATE TABLE IF NOT EXISTS github_webhook_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_key VARCHAR(64) NOT NULL COMMENT 'Webhook URL 경로용 사용자별 키 (예: webhooks/github/{tenantKey})',
    repo_full_name VARCHAR(256) NOT NULL COMMENT 'GitHub repository full name (owner/repo)',
    owner_user_id BIGINT NULL COMMENT '설정 소유 사용자 ID (감사/권한용)',
    body_prompt_id BIGINT NOT NULL COMMENT '본문 템플릿용 prompt ID',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1=활성, 0=비활성',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_tenant_repo (tenant_key, repo_full_name),
    KEY idx_github_webhook_config_tenant (tenant_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='GitHub Webhook 사용자별 설정 (tenantKey+repo → promptId)';

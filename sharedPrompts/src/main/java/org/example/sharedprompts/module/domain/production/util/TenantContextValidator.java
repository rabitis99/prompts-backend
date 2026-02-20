package org.example.sharedprompts.module.domain.production.util;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.tenant.TenantContext;

/**
 * 테넌트 컨텍스트 검증 유틸리티
 * 
 * 멀티 테넌트 SaaS 환경에서 비동기 작업 등에서 테넌트 컨텍스트가 설정되어 있는지 검증합니다.
 * tenant_id는 X-Tenant-Id 헤더에서 제공되어야 하며, 생성하거나 생성하지 않아야 합니다.
 */
@Slf4j
public final class TenantContextValidator {

    private TenantContextValidator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 테넌트 컨텍스트가 설정되어 있는지 검증합니다.
     * 
     * @param contextInfo 컨텍스트 정보 (예: jobId, operation name 등) - 로깅용
     * @return 검증된 tenant ID
     * @throws IllegalStateException 테넌트 컨텍스트가 설정되지 않은 경우
     */
    public static String requireTenantContext(String contextInfo) {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            String errorMessage = String.format(
                    "Tenant context is REQUIRED but not available - context: %s, thread: %s. " +
                    "Please ensure X-Tenant-Id header is provided.",
                    contextInfo, Thread.currentThread().getName());
            log.error(errorMessage);
            throw new IllegalStateException(
                    "Tenant context is required but not set. X-Tenant-Id header must be provided. Context: " + contextInfo);
        }
        log.debug("Tenant context verified - context: {}, tenantId: {}", contextInfo, tenantId);
        return tenantId;
    }

    /**
     * 테넌트 컨텍스트가 설정되어 있는지 검증합니다 (jobId 전용).
     * 
     * @param jobId Job ID
     * @return 검증된 tenant ID
     * @throws IllegalStateException 테넌트 컨텍스트가 설정되지 않은 경우
     */
    public static String requireTenantContextForJob(String jobId) {
        return requireTenantContext("jobId: " + jobId);
    }
}


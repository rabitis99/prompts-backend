package org.example.sharedprompts.module.domain.production.model.tenant;

public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static String getCurrentTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void setCurrentTenantId(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}

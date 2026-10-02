package com.example.spabooking.tenant.context;

public class TenantContext {

    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();

    public static void setTenantId(Long tenantId) {
        TENANT_ID.set(tenantId);
    }

    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    public static Long requireTenantId() {
        Long tenantId = getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("TenantContext cannot be resolved: tenant_id is missing");
        }
        return tenantId;
    }

    public static void clear() {
        TENANT_ID.remove();
    }
}

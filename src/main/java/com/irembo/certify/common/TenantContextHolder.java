package com.irembo.certify.common;

import java.util.UUID;

/**
 * Simple thread-local holder for the current tenant id, populated from the
 * authenticated user's JWT. This keeps services decoupled from HTTP details
 * while still enforcing tenant scoping.
 */
public final class TenantContextHolder {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void setTenantId(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static UUID getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}

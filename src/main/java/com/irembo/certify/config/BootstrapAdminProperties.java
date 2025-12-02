package com.irembo.certify.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for optionally bootstrapping initial admin users.
 * <p>
 * We seed both a platform-level SYSTEM_ADMIN account and a first TENANT_ADMIN
 * account for a specific tenant. All values should come from environment
 * variables or externalized configuration, never be hard-coded.
 */
@ConfigurationProperties(prefix = "certify.bootstrap.admin")
public record BootstrapAdminProperties(
        boolean enabled,
        // Tenant admin (merchant) configuration
        String tenantName,
        String tenantSlug,
        String email,
        String password,
        String fullName,
        // Platform/system admin configuration
        String systemEmail,
        String systemPassword,
        String systemFullName
) {
}

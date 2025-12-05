package com.irembo.certify.tenant;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.assertj.core.api.Assertions.assertThat;

class TenantAdminControllerSecurityTest {

    @Test
    void tenantAdminControllerIsRestrictedToSystemAdminRole() {
        PreAuthorize preAuthorize = TenantAdminController.class.getAnnotation(PreAuthorize.class);
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).contains("hasRole('SYSTEM_ADMIN')");
    }
}

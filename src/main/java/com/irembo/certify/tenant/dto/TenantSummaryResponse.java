package com.irembo.certify.tenant.dto;

import java.time.Instant;
import java.util.UUID;

public record TenantSummaryResponse(
        UUID id,
        String name,
        String slug,
        Instant createdAt,
        Instant updatedAt,
        TenantAdminContact admin
) {

    public record TenantAdminContact(String fullName, String email) {
    }
}

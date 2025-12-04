package com.irembo.certify.tenant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTenantRequest(
        @NotBlank(message = "Tenant name is required")
        String tenantName,
        String tenantSlug,
        @NotBlank(message = "Admin full name is required")
        String adminFullName,
        @Email(message = "Admin email must be valid")
        @NotBlank(message = "Admin email is required")
        String adminEmail,
        @NotBlank(message = "Admin password is required")
        @Size(min = 8, message = "Admin password must be at least 8 characters")
        String adminPassword
) {
}

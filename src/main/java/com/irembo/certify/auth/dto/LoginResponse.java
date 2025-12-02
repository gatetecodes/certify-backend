package com.irembo.certify.auth.dto;

import com.irembo.certify.user.Role;

import java.util.UUID;

public record LoginResponse(
        String accessToken,
        UUID userId,
        UUID tenantId,
        String fullName,
        String email,
        Role role
) {
}

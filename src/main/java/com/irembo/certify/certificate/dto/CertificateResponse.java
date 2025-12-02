package com.irembo.certify.certificate.dto;

import com.irembo.certify.certificate.CertificateStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CertificateResponse(
        UUID id,
        UUID templateId,
        UUID tenantId,
        CertificateStatus status,
        String storagePath,
        String hash,
        String createdBy,
        Instant createdAt,
        Map<String, Object> data,
        UUID verificationPublicId,
        String verificationUrl
) {
}

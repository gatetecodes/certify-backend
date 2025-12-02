package com.irembo.certify.certificate.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record CertificateGenerateRequest(
        @NotNull
        UUID templateId,

        @NotNull
        Map<String, Object> data
) {
}

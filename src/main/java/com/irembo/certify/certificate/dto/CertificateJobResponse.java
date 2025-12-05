package com.irembo.certify.certificate.dto;

import com.irembo.certify.certificate.CertificateJob;
import com.irembo.certify.certificate.CertificateJobStatus;

import java.time.Instant;
import java.util.UUID;

public record CertificateJobResponse(
        UUID id,
        UUID templateId,
        CertificateJobStatus status,
        UUID certificateId,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {

    public static CertificateJobResponse from(CertificateJob job) {
        return new CertificateJobResponse(
                job.getId(),
                job.getTemplateId(),
                job.getStatus(),
                job.getCertificateId(),
                job.getErrorMessage(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}

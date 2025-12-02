package com.irembo.certify.certificate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "certificate_verification_tokens")
public class CertificateVerificationToken {

    @Id
    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId;

    @Column(name = "certificate_id", nullable = false, updatable = false)
    private UUID certificateId;

    @Column(name = "checksum", nullable = false)
    private String checksum;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static CertificateVerificationToken create(UUID certificateId, String checksum, Instant expiresAt) {
        CertificateVerificationToken token = new CertificateVerificationToken();
        token.publicId = UUID.randomUUID();
        token.certificateId = certificateId;
        token.checksum = checksum;
        token.expiresAt = expiresAt;
        token.createdAt = Instant.now();
        return token;
    }
}

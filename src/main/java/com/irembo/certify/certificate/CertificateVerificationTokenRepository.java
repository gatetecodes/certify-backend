package com.irembo.certify.certificate;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CertificateVerificationTokenRepository extends JpaRepository<CertificateVerificationToken, UUID> {

    Optional<CertificateVerificationToken> findByPublicId(UUID publicId);

    Optional<CertificateVerificationToken> findByCertificateId(UUID certificateId);
}

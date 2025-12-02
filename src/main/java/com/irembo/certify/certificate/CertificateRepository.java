package com.irembo.certify.certificate;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CertificateRepository extends JpaRepository<Certificate, UUID> {

    List<Certificate> findAllByTenantId(UUID tenantId);

    Optional<Certificate> findByIdAndTenantId(UUID id, UUID tenantId);
}

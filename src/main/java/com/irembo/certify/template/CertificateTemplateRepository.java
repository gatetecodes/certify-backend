package com.irembo.certify.template;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CertificateTemplateRepository extends JpaRepository<CertificateTemplate, UUID> {

    List<CertificateTemplate> findAllByTenantId(UUID tenantId);

    Optional<CertificateTemplate> findByIdAndTenantId(UUID id, UUID tenantId);
}

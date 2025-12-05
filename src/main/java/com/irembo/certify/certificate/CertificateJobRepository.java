package com.irembo.certify.certificate;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CertificateJobRepository extends JpaRepository<CertificateJob, UUID> {

    List<CertificateJob> findTop50ByStatusOrderByCreatedAtAsc(CertificateJobStatus status);
}

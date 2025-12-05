package com.irembo.certify.certificate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CertificateJobRepository extends JpaRepository<CertificateJob, UUID> {

    /**
     * Claims a batch of jobs for processing using PostgreSQL row locking to avoid
     * multiple application instances picking the same rows.
     */
    @Query(
            value = """
                    SELECT *
                    FROM certificate_jobs
                    WHERE status = :status
                    ORDER BY created_at
                    FOR UPDATE SKIP LOCKED
                    LIMIT :limit
                    """,
            nativeQuery = true
    )
    List<CertificateJob> findNextBatchForUpdate(
            @Param("status") String status,
            @Param("limit") int limit
    );
}

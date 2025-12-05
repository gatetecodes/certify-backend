package com.irembo.certify.certificate;

import com.irembo.certify.common.TenantContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class CertificateJobProcessor {

    private static final Logger log = LoggerFactory.getLogger(CertificateJobProcessor.class);

    private final CertificateJobRepository jobRepository;
    private final CertificateService certificateService;

    public CertificateJobProcessor(
            CertificateJobRepository jobRepository,
            CertificateService certificateService
    ) {
        this.jobRepository = jobRepository;
        this.certificateService = certificateService;
    }

    /**
     * Looks in the certificate_jobs table for pending work and processes jobs in small batches.
     */
    @Scheduled(fixedDelayString = "${certify.jobs.poll-interval:1000}")
    public void pollAndProcessJobs() {
        List<UUID> claimedJobIds = claimNextBatch(50);
        for (UUID jobId : claimedJobIds) {
            try {
                processSingleJob(jobId);
            } catch (Exception ex) {
                log.error("Failed to process certificate job id={}", jobId, ex);
            }
        }
    }

    /**
     * Claims the next batch of PENDING jobs by locking and updating them to PROCESSING
     */
    @Transactional
    protected List<UUID> claimNextBatch(int limit) {
        List<CertificateJob> jobs = jobRepository.findNextBatchForUpdate(
                CertificateJobStatus.PENDING.name(),
                limit
        );

        for (CertificateJob job : jobs) {
            job.setStatus(CertificateJobStatus.PROCESSING);
        }

        return jobs.stream()
                .map(CertificateJob::getId)
                .collect(Collectors.toList());
    }

    @Transactional
    public void processSingleJob(UUID jobId) {
        CertificateJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null || job.getStatus() != CertificateJobStatus.PROCESSING) {
            return;
        }

        try {
            TenantContextHolder.setTenantId(job.getTenantId());
            var response = certificateService.processJob(job);
            job.setCertificateId(response.id());
            job.setStatus(CertificateJobStatus.COMPLETED);
            job.setErrorMessage(null);
        } catch (Exception ex) {
            log.warn("Certificate job id={} failed: {}", job.getId(), ex.getMessage());
            job.setStatus(CertificateJobStatus.FAILED);
            String message = ex.getMessage();
            if (message != null && message.length() > 500) {
                message = message.substring(0, 500);
            }
            job.setErrorMessage(message);
        } finally {
            TenantContextHolder.clear();
        }
    }
}

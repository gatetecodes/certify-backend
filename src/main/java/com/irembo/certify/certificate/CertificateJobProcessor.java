package com.irembo.certify.certificate;

import com.irembo.certify.common.TenantContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

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
        List<CertificateJob> jobs = jobRepository.findTop50ByStatusOrderByCreatedAtAsc(CertificateJobStatus.PENDING);
        for (CertificateJob job : jobs) {
            try {
                processSingleJob(job.getId());
            } catch (Exception ex) {
                log.error("Failed to process certificate job id={}", job.getId(), ex);
            }
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public void processSingleJob(UUID jobId) {
        CertificateJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null || job.getStatus() != CertificateJobStatus.PENDING) {
            return;
        }

        job.setStatus(CertificateJobStatus.PROCESSING);

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

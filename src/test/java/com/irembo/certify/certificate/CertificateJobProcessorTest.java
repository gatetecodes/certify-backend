package com.irembo.certify.certificate;

import com.irembo.certify.certificate.dto.CertificateResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificateJobProcessorTest {

    @Mock
    private CertificateJobRepository jobRepository;

    @Mock
    private CertificateService certificateService;

    @Test
    void processSingleJobCompletesSuccessfully() {
        UUID jobId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UUID certificateId = UUID.randomUUID();

        CertificateJob job = new CertificateJob();
        job.setId(jobId);
        job.setTenantId(tenantId);
        job.setTemplateId(templateId);
        job.setStatus(CertificateJobStatus.PROCESSING);
        job.setRequestDataJson("{}");
        job.setRequestedBy("user@example.com");

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        CertificateResponse certificateResponse = new CertificateResponse(
                certificateId,
                templateId,
                tenantId,
                CertificateStatus.GENERATED,
                "path",
                "hash",
                "user@example.com",
                Instant.now(),
                Map.of(),
                UUID.randomUUID(),
                "http://verify.test/public/verify/" + certificateId
        );
        when(certificateService.processJob(any(CertificateJob.class))).thenReturn(certificateResponse);

        CertificateJobProcessor processor = new CertificateJobProcessor(jobRepository, certificateService);
        processor.processSingleJob(jobId);

        assertThat(job.getStatus()).isEqualTo(CertificateJobStatus.COMPLETED);
        assertThat(job.getCertificateId()).isEqualTo(certificateId);
        assertThat(job.getErrorMessage()).isNull();
    }

    @Test
    void processSingleJobMarksFailedOnException() {
        UUID jobId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();

        CertificateJob job = new CertificateJob();
        job.setId(jobId);
        job.setTenantId(tenantId);
        job.setTemplateId(templateId);
        job.setStatus(CertificateJobStatus.PROCESSING);
        job.setRequestDataJson("{}");
        job.setRequestedBy("user@example.com");

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(certificateService.processJob(any(CertificateJob.class)))
                .thenThrow(new IllegalStateException("boom"));

        CertificateJobProcessor processor = new CertificateJobProcessor(jobRepository, certificateService);
        processor.processSingleJob(jobId);

        assertThat(job.getStatus()).isEqualTo(CertificateJobStatus.FAILED);
        assertThat(job.getErrorMessage()).contains("boom");
    }
}

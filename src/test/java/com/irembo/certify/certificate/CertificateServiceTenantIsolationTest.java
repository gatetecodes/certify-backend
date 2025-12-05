package com.irembo.certify.certificate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.irembo.certify.common.TenantContextHolder;
import com.irembo.certify.pdf.PdfRenderer;
import com.irembo.certify.qr.QrCodeService;
import com.irembo.certify.storage.FileSystemStorageService;
import com.irembo.certify.template.CertificateTemplateRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificateServiceTenantIsolationTest {

    @Mock
    private CertificateRepository certificateRepository;

    @Mock
    private CertificateVerificationTokenRepository tokenRepository;

    @Mock
    private CertificateTemplateRepository templateRepository;

    @Mock
    private CertificateJobRepository jobRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PdfRenderer pdfRenderer;

    @Mock
    private FileSystemStorageService storageService;

    @Mock
    private QrCodeService qrCodeService;

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void listForCurrentTenantUsesTenantFromContext() {
        UUID tenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        Certificate certificate = new Certificate();
        certificate.setTenantId(tenantId);
        certificate.setTemplateId(UUID.randomUUID());
        certificate.setStoragePath("path");
        certificate.setHash("hash");
        certificate.setStatus(CertificateStatus.GENERATED);
        certificate.setCreatedBy("user@example.com");
        certificate.setDataJson("{}");
        certificate.setId(UUID.randomUUID());
        certificate.setCreatedAt(Instant.now());
        certificate.setUpdatedAt(Instant.now());

        when(certificateRepository.findAllByTenantId(tenantId))
                .thenReturn(List.of(certificate));

        CertificateService service = new CertificateService(
                certificateRepository,
                tokenRepository,
                templateRepository,
                jobRepository,
                objectMapper,
                pdfRenderer,
                storageService,
                qrCodeService,
                "http://verify.test"
        );

        service.listForCurrentTenant();

        verify(certificateRepository).findAllByTenantId(tenantId);
    }

    @Test
    void listForCurrentTenantFailsWithoutTenant() {
        TenantContextHolder.clear();

        CertificateService service = new CertificateService(
                certificateRepository,
                tokenRepository,
                templateRepository,
                jobRepository,
                objectMapper,
                pdfRenderer,
                storageService,
                qrCodeService,
                "http://verify.test"
        );

        assertThatThrownBy(service::listForCurrentTenant)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No tenant in context");
    }
}

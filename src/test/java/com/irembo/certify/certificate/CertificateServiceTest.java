package com.irembo.certify.certificate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.irembo.certify.certificate.dto.CertificateGenerateRequest;
import com.irembo.certify.common.TenantContextHolder;
import com.irembo.certify.pdf.PdfRenderer;
import com.irembo.certify.qr.QrCodeService;
import com.irembo.certify.storage.FileSystemStorageService;
import com.irembo.certify.template.CertificateTemplate;
import com.irembo.certify.template.CertificateTemplateRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {

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
    void simulateInjectsVerificationUrlAndQrCode() {
        UUID tenantId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        CertificateTemplate template = new CertificateTemplate();
        template.setId(templateId);
        template.setTenantId(tenantId);
        template.setHtmlTemplate("<p>${verificationUrl}</p><img src='${qrCodeImage}' />");
        template.setPlaceholdersJson("[]");
        template.setName("T");
        template.setDescription(null);
        template.setActive(true);
        template.setVersion(1);

        when(templateRepository.findByIdAndTenantId(templateId, tenantId))
                .thenReturn(Optional.of(template));
        when(qrCodeService.generateDataUrl(anyString(), anyInt())).thenReturn("qr-data-url");
        when(pdfRenderer.renderHtmlToPdf(anyString())).thenReturn("pdf".getBytes());

        CertificateService service = new CertificateService(
                certificateRepository,
                tokenRepository,
                templateRepository,
                jobRepository,
                objectMapper,
                pdfRenderer,
                storageService,
                qrCodeService,
                "http://verify.test/public/verify"
        );

        Map<String, Object> data = new LinkedHashMap<>();
        CertificateGenerateRequest request = new CertificateGenerateRequest(templateId, data);

        byte[] result = service.simulate(request);
        assertThat(result).isEqualTo("pdf".getBytes());

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(pdfRenderer).renderHtmlToPdf(htmlCaptor.capture());
        String html = htmlCaptor.getValue();

        assertThat(html).contains("http://verify.test/public/verify/");
        assertThat(html).contains("qr-data-url");
    }

    @Test
    void generatePersistsCertificateAndSetsHashAndVerificationUrl() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        CertificateTemplate template = new CertificateTemplate();
        template.setId(templateId);
        template.setTenantId(tenantId);
        template.setHtmlTemplate("<p>Hello</p>");
        template.setPlaceholdersJson("[]");
        template.setName("T");
        template.setDescription(null);
        template.setActive(true);
        template.setVersion(1);

        when(templateRepository.findByIdAndTenantId(templateId, tenantId))
                .thenReturn(Optional.of(template));

        Certificate unsaved = new Certificate();
        unsaved.setTenantId(tenantId);
        unsaved.setTemplateId(templateId);
        unsaved.setCreatedBy("user@example.com");
        unsaved.setStatus(CertificateStatus.GENERATED);

        UUID certificateId = UUID.randomUUID();
        Certificate saved = new Certificate();
        saved.setTenantId(tenantId);
        saved.setTemplateId(templateId);
        saved.setCreatedBy("user@example.com");
        saved.setStatus(CertificateStatus.GENERATED);
        saved.setId(certificateId);
        saved.setDataJson("{}");
        saved.setStoragePath("pending");
        saved.setHash("pending");

        when(certificateRepository.save(any(Certificate.class))).thenReturn(saved);

        CertificateVerificationToken token = CertificateVerificationToken.create(certificateId, "pending", null);
        when(tokenRepository.save(any(CertificateVerificationToken.class))).thenReturn(token);
        when(tokenRepository.findByCertificateId(certificateId)).thenReturn(java.util.Optional.of(token));

        byte[] pdfBytes = "pdf-body".getBytes();
        when(pdfRenderer.renderHtmlToPdf(anyString())).thenReturn(pdfBytes);
        when(storageService.save(tenantId, certificateId, pdfBytes)).thenReturn("stored/path.pdf");

        CertificateService service = new CertificateService(
                certificateRepository,
                tokenRepository,
                templateRepository,
                jobRepository,
                objectMapper,
                pdfRenderer,
                storageService,
                qrCodeService,
                "http://verify.test/public/verify"
        );

        Map<String, Object> data = new LinkedHashMap<>();
        CertificateGenerateRequest request = new CertificateGenerateRequest(templateId, data);

        var response = service.generate(request, "user@example.com");

        byte[] expectedHashBytes = MessageDigest.getInstance("SHA-256").digest(pdfBytes);
        StringBuilder sb = new StringBuilder(expectedHashBytes.length * 2);
        for (byte b : expectedHashBytes) {
            sb.append(String.format("%02x", b));
        }
        String expectedHash = sb.toString();

        assertThat(response.hash()).isEqualTo(expectedHash);
        assertThat(response.storagePath()).isEqualTo("stored/path.pdf");
        assertThat(response.verificationUrl())
                .startsWith("http://verify.test/public/verify/");
    }
}

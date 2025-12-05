package com.irembo.certify.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.irembo.certify.common.TenantContextHolder;
import com.irembo.certify.pdf.PdfRenderer;
import com.irembo.certify.template.dto.TemplatePreviewRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {

    @Mock
    private CertificateTemplateRepository templateRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PdfRenderer pdfRenderer;

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void previewTemplateAppliesPlaceholders() {
        UUID tenantId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        CertificateTemplate template = new CertificateTemplate();
        template.setId(templateId);
        template.setTenantId(tenantId);
        template.setName("Test");
        template.setDescription("desc");
        template.setHtmlTemplate("<h1>Hello ${recipientName}</h1>");
        template.setPlaceholdersJson("[]");
        template.setActive(true);
        template.setVersion(1);

        when(templateRepository.findByIdAndTenantId(templateId, tenantId))
                .thenReturn(Optional.of(template));
        when(pdfRenderer.renderHtmlToPdf(anyString())).thenReturn("pdf".getBytes());

        TemplateService service = new TemplateService(templateRepository, objectMapper, pdfRenderer);

        TemplatePreviewRequest request = new TemplatePreviewRequest(Map.of("recipientName", "Alice"));
        byte[] result = service.previewTemplate(templateId, request);

        assertThat(result).isEqualTo("pdf".getBytes());

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(pdfRenderer).renderHtmlToPdf(htmlCaptor.capture());
        String renderedHtml = htmlCaptor.getValue();
        assertThat(renderedHtml).contains("Alice");
        assertThat(renderedHtml).doesNotContain("${recipientName}");
    }

    @Test
    void previewTemplateFailsWithoutTenant() {
        TenantContextHolder.clear();
        TemplateService service = new TemplateService(templateRepository, objectMapper, pdfRenderer);

        assertThatThrownBy(() ->
                service.previewTemplate(UUID.randomUUID(), new TemplatePreviewRequest(Map.of()))
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No tenant in context");
    }
}

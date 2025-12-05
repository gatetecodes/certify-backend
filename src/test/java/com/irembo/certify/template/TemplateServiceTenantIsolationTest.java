package com.irembo.certify.template;

import com.irembo.certify.common.TenantContextHolder;
import com.irembo.certify.pdf.PdfRenderer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateServiceTenantIsolationTest {

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
    void listForCurrentTenantUsesTenantFromContext() {
        UUID tenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        when(templateRepository.findAllByTenantId(tenantId)).thenReturn(List.of());

        TemplateService service = new TemplateService(templateRepository, objectMapper, pdfRenderer);

        service.listForCurrentTenant();

        verify(templateRepository).findAllByTenantId(tenantId);
    }

    @Test
    void listForCurrentTenantFailsWithoutTenant() {
        TenantContextHolder.clear();
        TemplateService service = new TemplateService(templateRepository, objectMapper, pdfRenderer);

        assertThatThrownBy(service::listForCurrentTenant)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No tenant in context");
    }
}

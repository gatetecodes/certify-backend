package com.irembo.certify.template;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.irembo.certify.common.TenantContextHolder;
import com.irembo.certify.pdf.PdfRenderer;
import com.irembo.certify.template.dto.PlaceholderDefinition;
import com.irembo.certify.template.dto.TemplatePreviewRequest;
import com.irembo.certify.template.dto.TemplateRequest;
import com.irembo.certify.template.dto.TemplateResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Transactional
public class TemplateService {

    private final CertificateTemplateRepository repository;
    private final ObjectMapper objectMapper;
    private final PdfRenderer pdfRenderer;

    private static final TypeReference<List<PlaceholderDefinition>> PLACEHOLDER_LIST_TYPE =
            new TypeReference<>() {};

    public TemplateService(
            CertificateTemplateRepository repository,
            ObjectMapper objectMapper,
            PdfRenderer pdfRenderer
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.pdfRenderer = pdfRenderer;
    }

    public List<TemplateResponse> listForCurrentTenant() {
        UUID tenantId = requireTenant();
        return repository.findAllByTenantId(tenantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TemplateResponse getForCurrentTenant(UUID id) {
        UUID tenantId = requireTenant();
        CertificateTemplate template = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Template not found"));
        return toResponse(template);
    }

    public TemplateResponse createTemplate(TemplateRequest request) {
        UUID tenantId = requireTenant();
        CertificateTemplate template = new CertificateTemplate();
        template.setTenantId(tenantId);
        template.setName(request.name());
        template.setDescription(request.description());
        template.setHtmlTemplate(request.htmlTemplate());
        template.setPlaceholdersJson(writePlaceholders(request.placeholders()));
        template.setActive(true);
        template.setVersion(1);
        CertificateTemplate saved = repository.save(template);
        return toResponse(saved);
    }

    public TemplateResponse updateTemplate(UUID id, TemplateRequest request) {
        UUID tenantId = requireTenant();
        CertificateTemplate template = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Template not found"));

        template.setName(request.name());
        template.setDescription(request.description());
        template.setHtmlTemplate(request.htmlTemplate());
        template.setPlaceholdersJson(writePlaceholders(request.placeholders()));
        template.setVersion(template.getVersion() + 1);

        return toResponse(template);
    }

    public void deactivateTemplate(UUID id) {
        UUID tenantId = requireTenant();
        CertificateTemplate template = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Template not found"));
        template.setActive(false);
    }

    public byte[] previewTemplate(UUID id, TemplatePreviewRequest request) {
        UUID tenantId = requireTenant();
        CertificateTemplate template = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Template not found"));

        String html = applyPlaceholders(template.getHtmlTemplate(), request.data());
        return pdfRenderer.renderHtmlToPdf(html);
    }

    private TemplateResponse toResponse(CertificateTemplate template) {
        List<PlaceholderDefinition> placeholders = readPlaceholders(template.getPlaceholdersJson());
        return new TemplateResponse(
                template.getId(),
                template.getName(),
                template.getDescription(),
                template.getHtmlTemplate(),
                template.isActive(),
                template.getVersion(),
                placeholders,
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }

    private List<PlaceholderDefinition> readPlaceholders(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json.getBytes(StandardCharsets.UTF_8), PLACEHOLDER_LIST_TYPE);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize placeholders", e);
        }
    }

    private String writePlaceholders(List<PlaceholderDefinition> placeholders) {
        try {
            return objectMapper.writeValueAsString(placeholders);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize placeholders", e);
        }
    }

    private UUID requireTenant() {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant in context");
        }
        return tenantId;
    }

    private String applyPlaceholders(String htmlTemplate, Map<String, Object> data) {
        String result = htmlTemplate;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }
}

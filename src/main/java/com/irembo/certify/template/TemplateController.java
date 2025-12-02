package com.irembo.certify.template;

import com.irembo.certify.template.dto.TemplatePreviewRequest;
import com.irembo.certify.template.dto.TemplateRequest;
import com.irembo.certify.template.dto.TemplateResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/templates")
public class TemplateController {

    private final TemplateService templateService;
    private static final Logger log = LoggerFactory.getLogger(TemplateController.class);

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','TENANT_USER')")
    public List<TemplateResponse> listTemplates() {
        return templateService.listForCurrentTenant();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','TENANT_USER')")
    public TemplateResponse getTemplate(@PathVariable("id") UUID id) {
        return templateService.getForCurrentTenant(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public TemplateResponse createTemplate(@Valid @RequestBody TemplateRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("createTemplate called by user={}, authorities={}",
                 auth.getName(), auth.getAuthorities());
        return templateService.createTemplate(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public TemplateResponse updateTemplate(@PathVariable("id") UUID id, @Valid @RequestBody TemplateRequest request) {
        return templateService.updateTemplate(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<Void> deactivateTemplate(@PathVariable("id") UUID id) {
        templateService.deactivateTemplate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/preview")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','TENANT_USER')")
    public ResponseEntity<byte[]> previewTemplate(
            @PathVariable("id") UUID id,
            @Valid @RequestBody TemplatePreviewRequest request
    ) {
        byte[] pdf = templateService.previewTemplate(id, request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=preview.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}

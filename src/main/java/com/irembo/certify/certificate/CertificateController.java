package com.irembo.certify.certificate;

import com.irembo.certify.certificate.dto.CertificateGenerateRequest;
import com.irembo.certify.certificate.dto.CertificateJobResponse;
import com.irembo.certify.certificate.dto.CertificateResponse;
import com.irembo.certify.certificate.dto.RevokeCertificateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/certificates")
public class CertificateController {

    private final CertificateService certificateService;

    public CertificateController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','TENANT_USER')")
    public List<CertificateResponse> listCertificates() {
        return certificateService.listForCurrentTenant();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','TENANT_USER')")
    public CertificateResponse getCertificate(@PathVariable("id") UUID id) {
        return certificateService.getForCurrentTenant(id);
    }

    @PostMapping("/simulate")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','TENANT_USER')")
    public ResponseEntity<byte[]> simulate(@Valid @RequestBody CertificateGenerateRequest request) {
        byte[] pdf = certificateService.simulate(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=certificate-preview.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','TENANT_USER')")
    public CertificateResponse generate(
            @Valid @RequestBody CertificateGenerateRequest request,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal
    ) {
        String createdByEmail = principal != null ? principal.getUsername() : "unknown";
        return certificateService.generate(request, createdByEmail);
    }

    @PostMapping("/async")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','TENANT_USER')")
    public CertificateJobResponse generateAsync(
            @Valid @RequestBody CertificateGenerateRequest request,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal
    ) {
        String requestedByEmail = principal != null ? principal.getUsername() : "unknown";
        var job = certificateService.submitAsyncJob(request, requestedByEmail);
        return CertificateJobResponse.from(job);
    }

    @GetMapping("/jobs/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','TENANT_USER')")
    public CertificateJobResponse getJob(@PathVariable("id") UUID id) {
        var job = certificateService.getJobForCurrentTenant(id);
        return CertificateJobResponse.from(job);
    }

    @PostMapping("/{id}/revoke")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public CertificateResponse revoke(
            @PathVariable("id") UUID id,
            @RequestBody(required = false) RevokeCertificateRequest request
    ) {
        String reason = request != null ? request.reason() : null;
        return certificateService.revokeForCurrentTenant(id, reason);
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','TENANT_USER')")
    public ResponseEntity<byte[]> download(@PathVariable("id") UUID id) {
        byte[] pdf = certificateService.downloadForCurrentTenant(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=certificate-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}

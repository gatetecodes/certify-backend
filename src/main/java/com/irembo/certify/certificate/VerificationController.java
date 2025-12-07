package com.irembo.certify.certificate;

import com.irembo.certify.certificate.dto.CertificateResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/public/verify")
public class VerificationController {

    private final CertificateVerificationTokenRepository tokenRepository;
    private final CertificateService certificateService;

    public VerificationController(
            CertificateVerificationTokenRepository tokenRepository,
            CertificateService certificateService
    ) {
        this.tokenRepository = tokenRepository;
        this.certificateService = certificateService;
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<?> verify(@PathVariable("publicId") UUID publicId) {
        var token = tokenRepository.findByPublicId(publicId);
        if (token.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        CertificateResponse certificate = certificateService.getById(token.get().getCertificateId());

        boolean hashMatches = certificate.hash().equals(token.get().getChecksum());

        String reason;
        if (!hashMatches) {
            reason = "HASH_MISMATCH";
        } else if (certificate.status() == CertificateStatus.REVOKED) {
            reason = "REVOKED";
        } else if (certificate.status() != CertificateStatus.GENERATED) {
            reason = "INVALID_STATUS";
        } else {
            reason = "OK";
        }

        boolean valid = "OK".equals(reason);

        return ResponseEntity.ok(Map.of(
                "valid", valid,
                "reason", reason,
                "certificateId", certificate.id(),
                "templateId", certificate.templateId(),
                "issuedAt", certificate.createdAt()
        ));
    }
}

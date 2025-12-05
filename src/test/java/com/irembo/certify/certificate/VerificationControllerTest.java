package com.irembo.certify.certificate;

import com.irembo.certify.certificate.dto.CertificateResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationControllerTest {

    @Mock
    private CertificateVerificationTokenRepository tokenRepository;

    @Mock
    private CertificateService certificateService;

    @Test
    void verifyReturnsOkWhenHashMatchesAndStatusGenerated() {
        UUID publicId = UUID.randomUUID();
        UUID certificateId = UUID.randomUUID();

        CertificateVerificationToken token = CertificateVerificationToken.create(certificateId, "hash", null);

        when(tokenRepository.findByPublicId(publicId)).thenReturn(Optional.of(token));

        CertificateResponse certificate = new CertificateResponse(
                certificateId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                CertificateStatus.GENERATED,
                "path",
                "hash",
                "user@example.com",
                Instant.now(),
                Map.of(),
                UUID.randomUUID(),
                "http://verify.test/public/verify/" + publicId
        );
        when(certificateService.getById(certificateId)).thenReturn(certificate);

        VerificationController controller = new VerificationController(tokenRepository, certificateService);

        ResponseEntity<?> responseEntity = controller.verify(publicId);
        Map<?, ?> body = (Map<?, ?>) responseEntity.getBody();

        assertThat(body).isNotNull();
        assertThat(body.get("valid")).isEqualTo(true);
        assertThat(body.get("reason")).isEqualTo("OK");
    }

    @Test
    void verifyReportsRevokedWhenStatusRevoked() {
        UUID publicId = UUID.randomUUID();
        UUID certificateId = UUID.randomUUID();

        CertificateVerificationToken token = CertificateVerificationToken.create(certificateId, "hash", null);
        when(tokenRepository.findByPublicId(publicId)).thenReturn(Optional.of(token));

        CertificateResponse certificate = new CertificateResponse(
                certificateId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                CertificateStatus.REVOKED,
                "path",
                "hash",
                "user@example.com",
                Instant.now(),
                Map.of(),
                UUID.randomUUID(),
                "http://verify.test/public/verify/" + publicId
        );
        when(certificateService.getById(certificateId)).thenReturn(certificate);

        VerificationController controller = new VerificationController(tokenRepository, certificateService);

        ResponseEntity<?> responseEntity = controller.verify(publicId);
        Map<?, ?> body = (Map<?, ?>) responseEntity.getBody();

        assertThat(body).isNotNull();
        assertThat(body.get("valid")).isEqualTo(false);
        assertThat(body.get("reason")).isEqualTo("REVOKED");
    }

    @Test
    void verifyReportsHashMismatch() {
        UUID publicId = UUID.randomUUID();
        UUID certificateId = UUID.randomUUID();

        CertificateVerificationToken token = CertificateVerificationToken.create(certificateId, "expected-hash", null);
        when(tokenRepository.findByPublicId(publicId)).thenReturn(Optional.of(token));

        CertificateResponse certificate = new CertificateResponse(
                certificateId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                CertificateStatus.GENERATED,
                "path",
                "different-hash",
                "user@example.com",
                Instant.now(),
                Map.of(),
                UUID.randomUUID(),
                "http://verify.test/public/verify/" + publicId
        );
        when(certificateService.getById(certificateId)).thenReturn(certificate);

        VerificationController controller = new VerificationController(tokenRepository, certificateService);

        ResponseEntity<?> responseEntity = controller.verify(publicId);
        Map<?, ?> body = (Map<?, ?>) responseEntity.getBody();

        assertThat(body).isNotNull();
        assertThat(body.get("valid")).isEqualTo(false);
        assertThat(body.get("reason")).isEqualTo("HASH_MISMATCH");
    }
}

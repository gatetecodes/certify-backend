package com.irembo.certify.certificate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.irembo.certify.certificate.dto.CertificateGenerateRequest;
import com.irembo.certify.certificate.dto.CertificateResponse;
import com.irembo.certify.common.TenantContextHolder;
import com.irembo.certify.pdf.PdfRenderer;
import com.irembo.certify.qr.QrCodeService;
import com.irembo.certify.storage.FileSystemStorageService;
import com.irembo.certify.template.CertificateTemplate;
import com.irembo.certify.template.CertificateTemplateRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
@Transactional
public class CertificateService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final CertificateRepository certificateRepository;
    private final CertificateVerificationTokenRepository tokenRepository;
    private final CertificateTemplateRepository templateRepository;
    private final ObjectMapper objectMapper;
    private final PdfRenderer pdfRenderer;
    private final FileSystemStorageService storageService;
    private final QrCodeService qrCodeService;
    private final String verificationBaseUrl;

    public CertificateService(
            CertificateRepository certificateRepository,
            CertificateVerificationTokenRepository tokenRepository,
            CertificateTemplateRepository templateRepository,
            ObjectMapper objectMapper,
            PdfRenderer pdfRenderer,
            FileSystemStorageService storageService,
            QrCodeService qrCodeService,
            @Value("${certify.verification.base-url:http://localhost:8080/public/verify}") String verificationBaseUrl
    ) {
        this.certificateRepository = certificateRepository;
        this.tokenRepository = tokenRepository;
        this.templateRepository = templateRepository;
        this.objectMapper = objectMapper;
        this.pdfRenderer = pdfRenderer;
        this.storageService = storageService;
        this.qrCodeService = qrCodeService;
        this.verificationBaseUrl = verificationBaseUrl;
    }

    public List<CertificateResponse> listForCurrentTenant() {
        UUID tenantId = requireTenant();
        return certificateRepository.findAllByTenantId(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    public CertificateResponse getForCurrentTenant(UUID id) {
        UUID tenantId = requireTenant();
        Certificate certificate = certificateRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Certificate not found"));
        return toResponse(certificate);
    }

    /**
     * Public lookup by id without tenant scoping. Used by verification flows where
     * we don't have an authenticated tenant context.
     */
    public CertificateResponse getById(UUID id) {
        Certificate certificate = certificateRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Certificate not found"));
        return toResponse(certificate);
    }

    public byte[] simulate(CertificateGenerateRequest request) {
        UUID tenantId = requireTenant();
        CertificateTemplate template = templateRepository
                .findByIdAndTenantId(request.templateId(), tenantId)
                .orElseThrow(() -> new NoSuchElementException("Template not found"));

        Map<String, Object> data = new LinkedHashMap<>(request.data());
        // Use a dummy public id just for preview QR code
        UUID dummyPublicId = UUID.randomUUID();
        String verificationUrl = verificationBaseUrl + "/" + dummyPublicId;
        String qrDataUrl = qrCodeService.generateDataUrl(verificationUrl, 200);
        data.putIfAbsent("qrCodeImage", qrDataUrl);

        String html = applyPlaceholders(template.getHtmlTemplate(), data);
        return pdfRenderer.renderHtmlToPdf(html);
    }

    public CertificateResponse generate(CertificateGenerateRequest request, String createdByEmail) {
        UUID tenantId = requireTenant();
        CertificateTemplate template = templateRepository
                .findByIdAndTenantId(request.templateId(), tenantId)
                .orElseThrow(() -> new NoSuchElementException("Template not found"));

        Map<String, Object> data = new LinkedHashMap<>(request.data());

        Certificate certificate = new Certificate();
        certificate.setTenantId(tenantId);
        certificate.setTemplateId(template.getId());
        certificate.setCreatedBy(createdByEmail);
        certificate.setStatus(CertificateStatus.GENERATED);

        // Satisfy NOT NULL constraints on initial insert;
        certificate.setDataJson(writeDataJson(data));
        certificate.setHash("pending");
        certificate.setStoragePath("pending");

        // Persist early to assign id that we can use for storage & verification
        certificate = certificateRepository.save(certificate);

        CertificateVerificationToken token = CertificateVerificationToken.create(certificate.getId(), "pending", null);
        token = tokenRepository.save(token);

        String verificationUrl = verificationBaseUrl + "/" + token.getPublicId();
        String qrDataUrl = qrCodeService.generateDataUrl(verificationUrl, 200);
        data.putIfAbsent("qrCodeImage", qrDataUrl);

        String html = applyPlaceholders(template.getHtmlTemplate(), data);
        byte[] pdfBytes = pdfRenderer.renderHtmlToPdf(html);

        String hash = sha256Hex(pdfBytes);
        String storagePath = storageService.save(tenantId, certificate.getId(), pdfBytes);

        certificate.setDataJson(writeDataJson(data));
        certificate.setHash(hash);
        certificate.setStoragePath(storagePath);

        token.setChecksum(hash);

        return toResponse(certificate);
    }

    public byte[] downloadForCurrentTenant(UUID id) {
        UUID tenantId = requireTenant();
        Certificate certificate = certificateRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Certificate not found"));
        return storageService.load(certificate.getStoragePath());
    }

    private CertificateResponse toResponse(Certificate certificate) {
        UUID tenantId = certificate.getTenantId();
        Map<String, Object> data = readDataJson(certificate.getDataJson());

        UUID publicId = tokenRepository.findByCertificateId(certificate.getId())
                .map(CertificateVerificationToken::getPublicId)
                .orElse(null);
        String verificationUrl = publicId != null ? verificationBaseUrl + "/" + publicId : null;

        return new CertificateResponse(
                certificate.getId(),
                certificate.getTemplateId(),
                tenantId,
                certificate.getStatus(),
                certificate.getStoragePath(),
                certificate.getHash(),
                certificate.getCreatedBy(),
                certificate.getCreatedAt(),
                data,
                publicId,
                verificationUrl
        );
    }

    private Map<String, Object> readDataJson(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json.getBytes(StandardCharsets.UTF_8), MAP_TYPE);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize certificate data", e);
        }
    }

    private String writeDataJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize certificate data", e);
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

    private String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

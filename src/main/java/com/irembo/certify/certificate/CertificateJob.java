package com.irembo.certify.certificate;

import com.irembo.certify.common.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "certificate_jobs")
public class CertificateJob extends BaseTenantEntity {

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_data_json", nullable = false, columnDefinition = "jsonb")
    private String requestDataJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CertificateJobStatus status = CertificateJobStatus.PENDING;

    @Column(name = "certificate_id")
    private UUID certificateId;

    @Column(name = "requested_by", nullable = false)
    private String requestedBy;

    @Column(name = "error_message")
    private String errorMessage;
}

package com.irembo.certify.certificate;

import com.irembo.certify.common.BaseTenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "certificates")
public class Certificate extends BaseTenantEntity {

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data_json", nullable = false, columnDefinition = "jsonb")
    private String dataJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CertificateStatus status = CertificateStatus.GENERATED;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Column(name = "hash", nullable = false)
    private String hash;

    @Column(name = "created_by", nullable = false)
    private String createdBy;
}

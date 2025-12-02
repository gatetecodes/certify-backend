package com.irembo.certify.template;

import com.irembo.certify.common.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "certificate_templates")
public class CertificateTemplate extends BaseTenantEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "html_template", nullable = false, columnDefinition = "text")
    private String htmlTemplate;

    // Store JSON as proper PostgreSQL jsonb. Hibernate will bind it as a JSON value.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "placeholders_json", nullable = false, columnDefinition = "jsonb")
    private String placeholdersJson;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "version", nullable = false)
    private int version = 1;
}

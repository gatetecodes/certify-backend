package com.irembo.certify.template.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TemplateResponse(
        UUID id,
        String name,
        String description,
        String htmlTemplate,
        boolean active,
        int version,
        List<PlaceholderDefinition> placeholders,
        Instant createdAt,
        Instant updatedAt
) {
}

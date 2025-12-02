package com.irembo.certify.template.dto;

import jakarta.validation.constraints.NotBlank;

public record PlaceholderDefinition(
        @NotBlank
        String key,

        @NotBlank
        String label,

        @NotBlank
        String type,

        boolean required
) {
}

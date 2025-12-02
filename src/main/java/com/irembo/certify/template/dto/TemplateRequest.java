package com.irembo.certify.template.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TemplateRequest(
        @NotBlank
        @Size(max = 255)
        String name,

        @Size(max = 1024)
        String description,

        @NotBlank
        String htmlTemplate,

        @NotEmpty
        List<PlaceholderDefinition> placeholders
) {
}

package com.irembo.certify.template.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record TemplatePreviewRequest(
        @NotNull
        Map<String, Object> data
) {
}

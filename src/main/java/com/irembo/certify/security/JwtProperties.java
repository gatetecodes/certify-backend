package com.irembo.certify.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "certify.security.jwt")
public record JwtProperties(
        String secret,
        long expirationMinutes
) {
}

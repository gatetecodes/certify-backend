package com.irembo.certify.security;

import com.irembo.certify.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        if (properties == null || properties.secret() == null || properties.secret().isBlank()) {
            throw new IllegalStateException(
                    "JWT secret is not configured. Set JWT_SECRET (certify.security.jwt.secret) before starting the application.");
        }
    }

    private byte[] signingKey() {
        return properties.secret().getBytes(StandardCharsets.UTF_8);
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(properties.expirationMinutes() * 60);

        return Jwts.builder()
                .setSubject(user.getEmail())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .addClaims(Map.of(
                        "tenantId", user.getTenantId().toString(),
                        "role", user.getRole().name(),
                        "userId", user.getId().toString(),
                        "fullName", user.getFullName()
                ))
                .signWith(Keys.hmacShaKeyFor(signingKey()), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(signingKey()))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public UUID extractTenantId(String token) {
        String value = (String) extractAllClaims(token).get("tenantId");
        return UUID.fromString(value);
    }
}

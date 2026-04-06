package com.condowhats.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades JWT lidas de condowhats.jwt.* no application.yml.
 * A secret deve ter no mínimo 256 bits (32 chars) para HS256.
 */
@ConfigurationProperties(prefix = "condowhats.jwt")
public record JwtProperties(
        String secret,
        long expirationMs,       // access token (ex: 3600000 = 1h)
        long refreshExpirationMs // refresh token (ex: 604800000 = 7d)
) {
}

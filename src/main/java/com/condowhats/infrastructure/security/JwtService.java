package com.condowhats.infrastructure.security;

import com.condowhats.domain.model.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties props;

    // ── Geração ───────────────────────────────────────────────────────────────

    public String generateAccessToken(AppUser user) {
        return buildToken(user, props.expirationMs(), Map.of(
                "role", user.getRole().name(),
                "condoId", user.getCondominium() != null ? user.getCondominium().getId() : "",
                "type", "access"
        ));
    }

    public String generateRefreshToken(AppUser user) {
        return buildToken(user, props.refreshExpirationMs(), Map.of("type", "refresh"));
    }

    // ── Validação ────────────────────────────────────────────────────────────

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("Token expirado: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("Token inválido: {}", e.getMessage());
        }
        return false;
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractType(String token) {
        return (String) parseClaims(token).get("type");
    }

    public Long extractCondoId(String token) {
        Object raw = parseClaims(token).get("condoId");
        if (raw == null) return null;
        return raw instanceof Integer i ? i.longValue() : (Long) raw;
    }

    public String extractRole(String token) {
        return (String) parseClaims(token).get("role");
    }

    // ── Internos ──────────────────────────────────────────────────────────────

    private String buildToken(AppUser user, long ttlMs, Map<String, Object> extraClaims) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getEmail())
                .claims(extraClaims)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMs))
                .signWith(secretKey())
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }
}

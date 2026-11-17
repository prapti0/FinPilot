package com.finpilot.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

public final class JwtSupport {
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_EMAIL = "X-User-Email";

    private JwtSupport() {}

    public static SecretKey key(String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(bytes, 0, padded, 0, Math.min(bytes.length, 32));
            return Keys.hmacShaKeyFor(padded);
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    public static String createToken(String secret, long userId, String email, String role, Instant expiresAt) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .addClaims(Map.of("email", email, "role", role))
                .setExpiration(Date.from(expiresAt))
                .signWith(key(secret))
                .compact();
    }

    public static Claims parse(String secret, String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key(secret))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
package com.signaltrellis.identity;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final Duration expiration;

    public JwtService(@Value("${security.jwt.secret}") String base64Secret,
                      @Value("${security.jwt.expiration}") Duration expiration) {
        this.signingKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64Secret));
        this.expiration = expiration;
    }

    public String issue(AuthenticatedUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.id().toString())
                .claim("email", user.email())
                .claim("workspace_id", user.workspaceId().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(signingKey)
                .compact();
    }

    public AuthenticatedUser parse(String token) {
        Claims claims = Jwts.parser().verifyWith(signingKey).build()
                .parseSignedClaims(token).getPayload();
        return new AuthenticatedUser(
                UUID.fromString(claims.getSubject()),
                UUID.fromString(claims.get("workspace_id", String.class)),
                claims.get("email", String.class), "");
    }

    public long expirationSeconds() {
        return expiration.toSeconds();
    }
}

package com.zjgsu.czh.userservice.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.zjgsu.czh.userservice.model.UserAccount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final byte[] secret;
    private final String issuer;
    private final Duration ttl;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.issuer:user-service}") String issuer,
            @Value("${app.jwt.ttl-minutes:120}") long ttlMinutes) {

        byte[] secretBytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);

        if (secretBytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes");
        }
        if (ttlMinutes <= 0) {
            throw new IllegalStateException("app.jwt.ttl-minutes must be positive");
        }

        this.secret = secretBytes;
        this.issuer = issuer;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    public String issue(UserAccount account) {
        Instant now = Instant.now();

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(account.getUsername())
                .claim("uid", account.getId())
                .claim("role", account.getRole())
                .issueTime(Date.from(now))
                .notBeforeTime(Date.from(now))
                .expirationTime(Date.from(now.plus(ttl)))
                .jwtID(UUID.randomUUID().toString())
                .build();

        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(secret));
            return jwt.serialize();
        } catch (JOSEException exception) {
            throw new IllegalStateException("Failed to issue JWT", exception);
        }
    }

    public JWTClaimsSet verify(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("Token is empty");
        }

        try {
            SignedJWT jwt = SignedJWT.parse(token);

            if (!jwt.verify(new MACVerifier(secret))) {
                throw new InvalidTokenException("Invalid token signature");
            }

            JWTClaimsSet claims = jwt.getJWTClaimsSet();

            if (!issuer.equals(claims.getIssuer())) {
                throw new InvalidTokenException("Invalid token issuer");
            }

            Date expiration = claims.getExpirationTime();
            if (expiration == null || expiration.before(new Date())) {
                throw new InvalidTokenException("Token expired");
            }

            if (claims.getSubject() == null || claims.getStringClaim("role") == null) {
                throw new InvalidTokenException("Token missing subject or role");
            }

            return claims;
        } catch (ParseException | JOSEException exception) {
            throw new InvalidTokenException("Failed to parse token", exception);
        }
    }

    public long expiresInSeconds() {
        return ttl.toSeconds();
    }
}

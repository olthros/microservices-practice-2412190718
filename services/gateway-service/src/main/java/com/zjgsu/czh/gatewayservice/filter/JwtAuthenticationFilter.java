package com.zjgsu.czh.gatewayservice.filter;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login",
            "/api/health",
            "/actuator/health",
            "/actuator/info",
            "/error");

    private static final Set<String> IDENTITY_HEADERS = Set.of(
            "X-User-Id",
            "X-User-Name",
            "X-User-Role",
            "X-User-Permissions",
            "X-Gateway-Token");

    private final byte[] secret;
    private final String issuer;

    public JwtAuthenticationFilter(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.issuer:user-service}") String issuer) {

        byte[] secretBytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);

        if (secretBytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes");
        }

        this.secret = secretBytes;
        this.issuer = issuer;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        ServerHttpRequest cleanRequest = request.mutate()
                .headers(headers -> IDENTITY_HEADERS.forEach(headers::remove))
                .build();
        ServerWebExchange cleanExchange = exchange.mutate()
                .request(cleanRequest)
                .build();

        if (HttpMethod.OPTIONS.equals(request.getMethod()) || isPublicPath(path)) {
            return chain.filter(cleanExchange);
        }

        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return unauthorized(cleanExchange, "Missing Bearer Token");
        }

        String token = authorization.substring(7).trim();

        try {
            SignedJWT jwt = SignedJWT.parse(token);

            if (!jwt.verify(new MACVerifier(secret))) {
                return unauthorized(cleanExchange, "Invalid token signature");
            }

            JWTClaimsSet claims = jwt.getJWTClaimsSet();

            if (!issuer.equals(claims.getIssuer())) {
                return unauthorized(cleanExchange, "Invalid token issuer");
            }

            Date expiration = claims.getExpirationTime();
            if (expiration == null || expiration.before(new Date())) {
                return unauthorized(cleanExchange, "Token expired");
            }

            Date notBefore = claims.getNotBeforeTime();
            if (notBefore != null && notBefore.toInstant().isAfter(Instant.now())) {
                return unauthorized(cleanExchange, "Token not active");
            }

            String username = claims.getSubject();
            String role = claims.getStringClaim("role");

            if (username == null || role == null || role.isBlank()) {
                return unauthorized(cleanExchange, "Token missing subject or role");
            }

            if (isAdminPath(path) && !"ADMIN".equals(role)) {
                log.warn("Forbidden admin access path={} username={} role={}", path, username, role);
                return forbidden(cleanExchange);
            }

            Object userId = claims.getClaim("uid");

            ServerHttpRequest authenticatedRequest = cleanRequest.mutate()
                    .headers(headers -> {
                        headers.set("X-User-Id", userId == null ? "" : String.valueOf(userId));
                        headers.set("X-User-Name", username);
                        headers.set("X-User-Role", role);
                    })
                    .build();

            return chain.filter(cleanExchange.mutate()
                    .request(authenticatedRequest)
                    .build());

        } catch (ParseException | JOSEException exception) {
            log.warn("JWT parse failed path={} reason={}", path, exception.getMessage());
            return unauthorized(cleanExchange, "Invalid token");
        }
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(publicPath ->
                path.equals(publicPath) || path.startsWith(publicPath + "/"));
    }

    private boolean isAdminPath(String path) {
        return path.equals("/api/admin") || path.startsWith("/api/admin/");
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        return writeError(exchange, HttpStatus.UNAUTHORIZED, message);
    }

    private Mono<Void> forbidden(ServerWebExchange exchange) {
        return writeError(exchange, HttpStatus.FORBIDDEN, "Admin role required");
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"status\":" + status.value()
                + ",\"error\":\"" + status.getReasonPhrase()
                + "\",\"message\":\"" + jsonEscape(message)
                + "\",\"path\":\"" + jsonEscape(exchange.getRequest().getURI().getPath())
                + "\"}";

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    private String jsonEscape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}

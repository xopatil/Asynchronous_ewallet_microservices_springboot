package com.sterling.api_gateway.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

// GlobalFilter: runs on EVERY request that passes through the Gateway.
// This is the reactive equivalent of Spring MVC's OncePerRequestFilter.
// "Reactive" because the Gateway uses WebFlux (non-blocking).
// Ordered: controls filter priority. Lower number = runs first.
// We set -1 so this runs before all other filters.
@Slf4j
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtUtil jwtUtil;

    // Read public paths from application.properties
    @Value("${gateway.public-paths}")
    private String publicPathsConfig;

    @Override
    public int getOrder() {
        // -1 means this filter runs first — before routing happens
        return -1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        log.debug("Gateway JwtAuthFilter processing path: {}", path);

        // Check if this path is public (no token needed)
        List<String> publicPaths = Arrays.asList(publicPathsConfig.split(","));
        boolean isPublic = publicPaths.stream()
                .anyMatch(publicPath -> path.startsWith(publicPath.trim()));

        if (isPublic) {
            log.debug("Public path — skipping JWT validation: {}", path);
            // Pass through without checking token
            return chain.filter(exchange);
        }

        // Path requires authentication — check for Bearer token
        String authHeader = request.getHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            return unauthorizedResponse(exchange, "Missing Authorization header");
        }

        String token = authHeader.substring(7);

        // Validate the token
        if (!jwtUtil.validateToken(token)) {
            log.warn("Invalid or expired JWT token for path: {}", path);
            return unauthorizedResponse(exchange, "Invalid or expired token");
        }

        // Token is valid — extract username and add to request headers
        // Downstream services can read X-Username header to know who is logged in
        String username = jwtUtil.extractUsername(token);
        log.debug("JWT valid for username: {} — forwarding to service", username);

        // Add username as a header so downstream services know who made the request
        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-Username", username)
                .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    // Helper: send 401 Unauthorized response and stop the request
    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");

        // Write a JSON error body
        String body = "{\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}";
        org.springframework.core.io.buffer.DataBuffer buffer =
                response.bufferFactory().wrap(body.getBytes());

        return response.writeWith(Mono.just(buffer));
    }
}
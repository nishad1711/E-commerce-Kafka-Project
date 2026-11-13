
package com.Ecommerce.product_service.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;

@Component
public class ProductAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final String internalServiceToken;

    public ProductAuthenticationFilter(
            JwtService jwtService,
            @Value("${app.internal-service.token}") String internalServiceToken
    ) {
        this.jwtService = jwtService;
        this.internalServiceToken = internalServiceToken;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        String contextPath = request.getContextPath();

        if (contextPath != null && !contextPath.isEmpty()
                && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        boolean internalEndpoint = path.matches(
                "^/products/\\d+/(reserve-stock|release-stock)$"
        );

        if (internalEndpoint) {

            String suppliedToken =
                    request.getHeader("X-Internal-Service-Token");

            if (suppliedToken != null
                    && !internalServiceToken.isBlank()
                    && MessageDigest.isEqual(
                    suppliedToken.getBytes(StandardCharsets.UTF_8),
                    internalServiceToken.getBytes(StandardCharsets.UTF_8)
            )) {

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                "order-service",
                                null,
                                Collections.singletonList(
                                        new SimpleGrantedAuthority("ROLE_SERVICE")
                                )
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
            }

        } else {

            String authHeader = request.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {

                String token = authHeader.substring(7);

                try {
                    Claims claims = jwtService.extractAllClaims(token);

                    String email = claims.getSubject();
                    String role = claims.get("role", String.class);

                    if (email != null && !email.isBlank()
                            && ("USER".equals(role) || "ADMIN".equals(role))) {

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        email,
                                        null,
                                        Collections.singletonList(
                                                new SimpleGrantedAuthority("ROLE_" + role)
                                        )
                                );

                        authentication.setDetails(
                                new WebAuthenticationDetailsSource()
                                        .buildDetails(request)
                        );

                        SecurityContextHolder.getContext()
                                .setAuthentication(authentication);

                    } else {
                        SecurityContextHolder.clearContext();
                    }

                } catch (Exception exception) {
                    SecurityContextHolder.clearContext();
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}

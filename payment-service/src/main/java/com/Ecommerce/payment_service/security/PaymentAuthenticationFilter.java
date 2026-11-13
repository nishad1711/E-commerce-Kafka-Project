
package com.Ecommerce.payment_service.security;

import com.Ecommerce.payment_service.security.UserPrincipal;
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
import java.util.List;

@Component
public class PaymentAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final String internalServiceToken;

    public PaymentAuthenticationFilter(
            JwtService jwtService,
            @Value("${app.internal-service.token}") String internalServiceToken) {

        this.jwtService = jwtService;
        this.internalServiceToken = internalServiceToken;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String contextPath = request.getContextPath();

        if (contextPath != null
                && !contextPath.isEmpty()
                && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        boolean internalEndpoint =
                path.equals("/payments")
                        || path.matches("^/payments/\\d+/status$");

        try {
            if (internalEndpoint) {

                String suppliedToken =
                        request.getHeader("X-Internal-Service-Token");

                boolean validToken = suppliedToken != null
                        && !internalServiceToken.isBlank()
                        && MessageDigest.isEqual(
                        suppliedToken.getBytes(StandardCharsets.UTF_8),
                        internalServiceToken.getBytes(StandardCharsets.UTF_8)
                );

                if (validToken) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    "order-service",
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_SERVICE"))
                            );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    SecurityContextHolder.getContext()
                            .setAuthentication(authentication);
                }

            } else {

                String authHeader =
                        request.getHeader("Authorization");

                if (authHeader != null
                        && authHeader.startsWith("Bearer ")) {

                    String token = authHeader.substring(7);
                    Claims claims = jwtService.extractAllClaims(token);

                    String email = claims.getSubject();
                    String role = claims.get("role", String.class);
                    String userId = claims.get("userId", String.class);

                    if (email != null
                            && !email.isBlank()
                            && userId != null
                            && !userId.isBlank()
                            && ("USER".equals(role) || "ADMIN".equals(role))) {

                        UserPrincipal principal = new UserPrincipal(
                                Long.parseLong(userId),
                                email,
                                role
                        );

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        principal,
                                        null,
                                        List.of(
                                                new SimpleGrantedAuthority("ROLE_" + role)
                                        )
                                );

                        authentication.setDetails(
                                new WebAuthenticationDetailsSource()
                                        .buildDetails(request)
                        );

                        SecurityContextHolder.getContext()
                                .setAuthentication(authentication);
                    }
                }
            }

        } catch (Exception exception) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}

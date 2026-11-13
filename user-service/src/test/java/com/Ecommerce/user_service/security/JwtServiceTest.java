package com.Ecommerce.user_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET =
            "this-is-a-very-long-test-secret-key-for-jwt-testing-123456";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET);
    }

    @Test
    void shouldGenerateToken() {

        String token = jwtService.generateToken(
                1L,
                "test@example.com",
                "USER"
        );

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void shouldExtractEmail() {

        String token = jwtService.generateToken(
                1L,
                "test@example.com",
                "USER"
        );

        assertEquals(
                "test@example.com",
                jwtService.extractEmail(token)
        );
    }

    @Test
    void shouldExtractRole() {

        String token = jwtService.generateToken(
                1L,
                "test@example.com",
                "ADMIN"
        );

        assertEquals("ADMIN", jwtService.extractRole(token));
    }

    @Test
    void shouldValidateCorrectToken() {

        String token = jwtService.generateToken(
                1L,
                "test@example.com",
                "USER"
        );

        assertTrue(jwtService.isValid(token));
    }

    @Test
    void shouldRejectExpiredToken() {

        SecretKey key = Keys.hmacShaKeyFor(
                SECRET.getBytes(StandardCharsets.UTF_8)
        );

        String expiredToken = Jwts.builder()
                .subject("test@example.com")
                .claim("role", "USER")
                .issuedAt(new Date(System.currentTimeMillis() - 7200000))
                .expiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(key)
                .compact();

        assertFalse(jwtService.isValid(expiredToken));
    }

    @Test
    void shouldRejectTokenWithInvalidSignature() {

        String token = jwtService.generateToken(
                1L,
                "test@example.com",
                "USER"
        );
        JwtService anotherJwtService = new JwtService(
                "another-very-long-test-secret-key-for-jwt-testing-456789"
        );

        assertFalse(anotherJwtService.isValid(token));
    }

    @Test
    void shouldRejectMalformedToken() {

        assertFalse(jwtService.isValid("this-is-not-a-valid-jwt"));
    }
}

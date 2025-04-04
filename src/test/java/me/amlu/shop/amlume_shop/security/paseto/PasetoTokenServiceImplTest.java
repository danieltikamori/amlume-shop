/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.paseto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import jakarta.servlet.http.HttpServletRequest;
import me.amlu.shop.amlume_shop.exceptions.TokenGenerationFailureException;
import me.amlu.shop.amlume_shop.exceptions.TokenValidationFailureException;
import me.amlu.shop.amlume_shop.model.RefreshToken;
import me.amlu.shop.amlume_shop.user_management.User;
import me.amlu.shop.amlume_shop.payload.user.AuthResponse;
import me.amlu.shop.amlume_shop.repositories.RefreshTokenRepository;
import me.amlu.shop.amlume_shop.security.enums.TokenType;
import me.amlu.shop.amlume_shop.security.repository.RevokedTokenRepository;
import me.amlu.shop.amlume_shop.security.service.EnhancedAuthenticationService;
import me.amlu.shop.amlume_shop.user_management.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.paseto4j.commons.SecretKey;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasetoTokenServiceImplTest {

    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private EnhancedAuthenticationService enhancedAuthenticationService;
    @Mock
    private RevokedTokenRepository revokedTokenRepository;
    @Mock
    private Cache<String, Boolean> revokedTokensCache;
    @Mock
    private KeyFactory keyFactory;
    @Mock
    private PrivateKey accessPrivateKey;
    @Mock
    private PublicKey accessPublicKey;
    @Mock
    private SecretKey accessSecretKey;
    @Mock
    private SecretKey refreshSecretKey;
    @Mock
    private SecureRandom secureRandom;


    @InjectMocks
    private PasetoTokenServiceImpl pasetoTokenService;

    private final String PASETO_ACCESS_PRIVATE_KEY = "privateKey";
    private final String PASETO_ACCESS_PUBLIC_KEY = "publicKey";
    private final String PASETO_ACCESS_SECRET_KEY = "accessSecretKey";
    private final String PASETO_REFRESH_SECRET_KEY = "refreshSecretKey";
    private final String PASETO_WRAPPED_PASERK = "wpk";
    private final String KEY_ID = "keyId";

    @Test
    public void testKeyConversion() {
        String privateKeyString = "MC4CAQAwBQYDK2VwBCIEIOFfI2fecyMLXnv4e/M0iyxP0pdLf1aBNSiqrCCL/6Ra";
        String publicKeyString = "MCowBQYDK2VwAyEAjAHSnKHGcWygrd85kgmN+x3TTaUIWa1wFMCs/DGKi58";

        try {
            PrivateKey privateKey = convertToPrivateKey(privateKeyString);
            PublicKey publicKey = convertToPublicKey(publicKeyString);

            // Create a test payload
            String payload = "test message";

            // Sign with private key
            byte[] signature = privateKey.sign(payload.getBytes());

            // Verify with public key
            boolean isValid = publicKey.verify(payload.getBytes(), signature);

            assert isValid : "Signature verification failed";

        } catch (Exception e) {
            fail("Key conversion failed: " + e.getMessage());
        }
    }

    @Test
    void generatePublicAccessToken_happyPath() throws Exception {
        String userId = "1";
        Duration duration = Duration.ofMinutes(30);
        User user = new User();
        user.setUserId(1L);

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
        when(enhancedAuthenticationService.determineUserScope()).thenReturn("user");
        when(httpServletRequest.getSession()).thenReturn(mock(jakarta.servlet.http.HttpSession.class));
        when(httpServletRequest.getSession().getId()).thenReturn("sessionId");
        when(secureRandom.nextBytes(any(byte[].class))).thenAnswer(invocation -> {
            byte[] bytes = new byte[32]; // Example size
            secureRandom.nextBytes(bytes); // Fill with random data
            return null; // Or return the byte array if needed
        });

        // Act
        String token = pasetoTokenService.generatePublicAccessToken(userId, duration);

        // Assert
        assertNotNull(token);
    }


    @Test
    void generatePublicAccessToken_payloadTooLarge() throws JsonProcessingException {

        // Arrange
        String userId = "1";
        Duration duration = Duration.ofMinutes(30);
        User user = new User();
        user.setUserId(1L);
        ObjectMapper objectMapper = new ObjectMapper();

        String longPayload = "very_long_payload".repeat(1000000); // Create a payload larger than 1MB
        PasetoClaims claims = new PasetoClaims();
        claims.addClaim("longPayload", longPayload);
        String payload = objectMapper.writeValueAsString(claims);

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
        when(enhancedAuthenticationService.determineUserScope()).thenReturn("user");
        when(httpServletRequest.getSession()).thenReturn(mock(jakarta.servlet.http.HttpSession.class));
        when(httpServletRequest.getSession().getId()).thenReturn("sessionId");
        when(secureRandom.nextBytes(any(byte[].class))).thenAnswer(invocation -> {
            byte[] bytes = new byte[32]; // Example size
            secureRandom.nextBytes(bytes); // Fill with random data
            return null; // Or return the byte array if needed
        });

        // Act & Assert
        assertThrows(TokenGenerationFailureException.class, () -> pasetoTokenService.validatePayload(payload));
    }

    @Test
    void generatePublicAccessToken_userNotFound() {
        // Arrange
        String userId = "1";
        Duration duration = Duration.ofMinutes(30);

        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () -> pasetoTokenService.generatePublicAccessToken(userId, duration));
    }


    @Test
    void generateLocalAccessToken_happyPath() throws Exception {
        String userId = "1";
        Duration duration = Duration.ofMinutes(30);
        User user = new User();
        user.setUserId(1L);

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
        when(enhancedAuthenticationService.determineUserScope()).thenReturn("user");
        when(httpServletRequest.getSession()).thenReturn(mock(jakarta.servlet.http.HttpSession.class));
        when(httpServletRequest.getSession().getId()).thenReturn("sessionId");
        when(secureRandom.nextBytes(any(byte[].class))).thenAnswer(invocation -> {
            byte[] bytes = new byte[32]; // Example size
            secureRandom.nextBytes(bytes); // Fill with random data
            return null; // Or return the byte array if needed
        });

        // Act
        String token = pasetoTokenService.generateLocalAccessToken(userId, duration);

        // Assert
        assertNotNull(token);
    }

    @Test
    void generateRefreshToken() {

        // Arrange
        User user = new User();
        user.setUserId(1L);
        when(enhancedAuthenticationService.getRefreshTokenDuration()).thenReturn(Duration.ofDays(7));
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("test-agent");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        String refreshToken = pasetoTokenService.generateRefreshToken(user);

        // Assert
        assertNotNull(refreshToken);
    }

    @Test
    void validatePublicAccessToken_happyPath() throws Exception {

        // Arrange
        String userId = "1";
        Duration duration = Duration.ofMinutes(30);
        User user = new User();
        user.setUserId(1L);
        String token = pasetoTokenService.generatePublicAccessToken(userId, duration);

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
        when(enhancedAuthenticationService.determineUserScope()).thenReturn("user");
        when(httpServletRequest.getSession()).thenReturn(mock(jakarta.servlet.http.HttpSession.class));
        when(httpServletRequest.getSession().getId()).thenReturn("sessionId");
        when(secureRandom.nextBytes(any(byte[].class))).thenAnswer(invocation -> {
            byte[] bytes = new byte[32]; // Example size
            secureRandom.nextBytes(bytes); // Fill with random data
            return null; // Or return the byte array if needed
        });

        // Act
        Map<String, Object> claims = pasetoTokenService.validatePublicAccessToken(token);

        // Assert
        assertEquals(userId, claims.get("sub"));
    }


    @Test
    void validateLocalAcessToken_happyPath() throws Exception {

        // Arrange
        String userId = "1";
        Duration duration = Duration.ofMinutes(30);
        User user = new User();
        user.setUserId(1L);
        String token = pasetoTokenService.generateLocalAccessToken(userId, duration);

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
        when(enhancedAuthenticationService.determineUserScope()).thenReturn("user");
        when(httpServletRequest.getSession()).thenReturn(mock(jakarta.servlet.http.HttpSession.class));
        when(httpServletRequest.getSession().getId()).thenReturn("sessionId");
        when(secureRandom.nextBytes(any(byte[].class))).thenAnswer(invocation -> {
            byte[] bytes = new byte[32]; // Example size
            secureRandom.nextBytes(bytes); // Fill with random data
            return null; // Or return the byte array if needed
        });

        // Act
        Map<String, Object> claims = pasetoTokenService.validateLocalAcessToken(token);

        // Assert
        assertEquals(userId, claims.get("sub"));
    }


    @Test
    void validateLocalRefreshToken_happyPath() throws Exception {

        // Arrange
        User user = new User();
        user.setUserId(1L);
        String token = pasetoTokenService.generateRefreshToken(user);
        when(enhancedAuthenticationService.getRefreshTokenDuration()).thenReturn(Duration.ofDays(7));
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("test-agent");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Map<String, Object> claims = pasetoTokenService.validateLocalRefreshToken(token);

        // Assert
        assertEquals("1", claims.get("sub"));
    }


    @Test
    void createAccessPasetoClaims() {

        // Arrange
        String userId = "1";
        Duration duration = Duration.ofMinutes(30);
        User user = new User();
        user.setUserId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(enhancedAuthenticationService.determineUserScope()).thenReturn("test_scope");
        when(httpServletRequest.getSession().getId()).thenReturn("test_session_id");

        // Act
        PasetoClaims claims = pasetoTokenService.createAccessPasetoClaims(userId, duration);

        // Assert
        assertAll(
                () -> assertEquals(PasetoTokenServiceImpl.DEFAULT_ISSUER, claims.getIssuer()),
                () -> assertEquals(userId, claims.getSubject()),
                () -> assertEquals(PasetoTokenServiceImpl.DEFAULT_AUDIENCE, claims.getAudience()),
                () -> assertNotNull(claims.getExpiration()),
                () -> assertNotNull(claims.getNotBefore()),
                () -> assertNotNull(claims.getIssuedAt()),
                () -> assertNotNull(claims.getTokenId()),
                () -> assertEquals("test_session_id", claims.getSessionId()),
                () -> assertEquals("test_scope", claims.getScope()),
                () -> assertEquals(TokenType.ACCESS_TOKEN.toString(), claims.getTokenType())
        );
    }

    @Test
    void createRefreshPasetoClaims() {

        // Arrange
        String userId = "1";
        Duration duration = Duration.ofMinutes(30);
        User user = new User();
        user.setUserId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act
        PasetoClaims claims = pasetoTokenService.createRefreshPasetoClaims(userId, duration);

        // Assert
        assertAll(
                () -> assertEquals(PasetoTokenServiceImpl.DEFAULT_ISSUER, claims.getIssuer()),
                () -> assertEquals(userId, claims.getSubject()),
                () -> assertEquals(PasetoTokenServiceImpl.DEFAULT_AUDIENCE, claims.getAudience()),
                () -> assertNotNull(claims.getExpiration()),
                () -> assertNotNull(claims.getNotBefore()),
                () -> assertNotNull(claims.getIssuedAt()),
                () -> assertNotNull(claims.getTokenId()),
                () -> assertEquals(TokenType.REFRESH_TOKEN.toString(), claims.getTokenType())
        );
    }

    @Test
    void createPasetoFooterClaims() {

        // Arrange
        String keyId = "keyId";
        String wrappedPaserk = "wrappedPaserk";

        // Act
        PasetoClaims claims = pasetoTokenService.createPasetoFooterClaims(keyId, wrappedPaserk);

        // Assert
        assertAll(
                () -> assertEquals(keyId, claims.getKeyId()),
                () -> assertEquals(wrappedPaserk, claims.getWrappedPaserk())
        );
    }


    @Test
    void validateAccessTokenClaims_happyPath() throws TokenValidationFailureException {
        // Arrange
        Map<String, Object> claims = new HashMap<>();
        Instant now = Instant.now();
        claims.put("exp", now.plusSeconds(30).toString());
        claims.put("nbf", now.minusSeconds(30).toString());
        claims.put("iat", now.minusSeconds(30).toString());

        // Act
        pasetoTokenService.validateAccessTokenClaims(claims, String.valueOf(TokenType.ACCESS_TOKEN));

        // Assert - No exception thrown means success
    }


    @Test
    void validateLocalRefreshTokenClaims_happyPath() throws TokenValidationFailureException {
        // Arrange
        Map<String, Object> claims = new HashMap<>();
        Instant now = Instant.now();
        claims.put("exp", now.plusSeconds(30).toString());
        claims.put("nbf", now.minusSeconds(30).toString());
        claims.put("iat", now.minusSeconds(30).toString());
        claims.put("sub", "1");

        User user = new User();
        user.setUserId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act
        pasetoTokenService.validateRefreshTokenClaims(claims, String.valueOf(TokenType.REFRESH_TOKEN));

        // Assert - No exception thrown means success
    }


    @Test
    void validatePayload_happyPath() throws TokenGenerationFailureException {
        // Arrange
        String payload = "test_payload";

        // Act
        pasetoTokenService.validatePayload(payload);

        // Assert - No exception thrown means success
    }


    @Test
    void isTokenExpired() throws TokenValidationFailureException {
        // Arrange
        String userId = "1";
        Duration duration = Duration.ofMinutes(30);
        User user = new User();
        user.setUserId(1L);
        String token = pasetoTokenService.generatePublicAccessToken(userId, duration);

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
        when(enhancedAuthenticationService.determineUserScope()).thenReturn("user");
        when(httpServletRequest.getSession()).thenReturn(mock(jakarta.servlet.http.HttpSession.class));
        when(httpServletRequest.getSession().getId()).thenReturn("sessionId");
        when(secureRandom.nextBytes(any(byte[].class))).thenAnswer(invocation -> {
            byte[] bytes = new byte[32]; // Example size
            secureRandom.nextBytes(bytes); // Fill with random data
            return null; // Or return the byte array if needed
        });

        // Act
        boolean isExpired = pasetoTokenService.isTokenExpired(token);

        // Assert
        assertFalse(isExpired);
    }


    @Test
    void refreshAccessToken_happyPath() throws Exception {
        // Arrange
        User user = new User();
        user.setUserId(1L);
        String refreshToken = pasetoTokenService.generateRefreshToken(user);
        String hashedRefreshToken = BCrypt.hashpw(refreshToken, BCrypt.gensalt());
        RefreshToken refreshTokenEntity = new RefreshToken();
        refreshTokenEntity.setToken(hashedRefreshToken);
        refreshTokenEntity.setUser(user);
        when(enhancedAuthenticationService.getRefreshTokenDuration()).thenReturn(Duration.ofDays(7));
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("test-agent");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArguments()[0]);
        when(refreshTokenRepository.findByToken(hashedRefreshToken)).thenReturn(Optional.of(refreshTokenEntity));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(enhancedAuthenticationService.getAccessTokenDuration()).thenReturn(Duration.ofMinutes(30));

        // Act
        AuthResponse authResponse = pasetoTokenService.refreshAccessToken(refreshToken);

        // Assert
        assertNotNull(authResponse);
        assertNotNull(authResponse.getAccessToken());
        assertNotNull(authResponse.getRefreshToken());
    }

}

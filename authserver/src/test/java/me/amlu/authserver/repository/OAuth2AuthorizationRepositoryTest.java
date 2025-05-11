/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.repository;

import me.amlu.authserver.oauth2.model.OAuth2Authorization;
import me.amlu.authserver.oauth2.repository.OAuth2AuthorizationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.IncorrectResultSizeDataAccessException; // For specific test case

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
// If your OAuth2Authorization entity is not in a sub-package of your main application class,
// or if @DataJpaTest doesn't pick it up, you might need:
// @org.springframework.boot.autoconfigure.domain.EntityScan(basePackageClasses = OAuth2Authorization.class)
public class OAuth2AuthorizationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OAuth2AuthorizationRepository repository;

    private OAuth2Authorization auth1;
    private OAuth2Authorization auth2;

    private String auth1Id;
    private String auth2Id;

    @BeforeEach
    void setUp() {
        auth1Id = UUID.randomUUID().toString();
        auth1 = OAuth2Authorization.builder() // Using the builder from your actual entity
                .id(auth1Id)
                .principalName("user1@example.com")
                .registeredClientId("client-id-1")
                .authorizationGrantType("authorization_code")
                .state("state123")
                .authorizationCodeValue("codeABC")
                .accessTokenValue("accessTokenXYZ")
                .refreshTokenValue("refreshToken123")
                .userCodeValue("userCodeDEF")
                .deviceCodeValue("deviceCodeGHI")
                .build();

        auth2Id = UUID.randomUUID().toString();
        auth2 = OAuth2Authorization.builder() // Using the builder from your actual entity
                .id(auth2Id)
                .principalName("user2@example.com")
                .registeredClientId("client-id-2")
                .authorizationGrantType("client_credentials")
                .state("state456")
                .build();

        entityManager.persist(auth1);
        entityManager.persist(auth2);
        entityManager.flush();
    }

    @AfterEach
    void tearDown() {
        // @DataJpaTest typically handles transaction rollback.
        // No explicit cleanup is usually needed here.
    }

    @Test
    void findByState_whenStateExists_thenReturnAuthorization() {
        Optional<OAuth2Authorization> found = repository.findByState("state123");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(auth1Id);
        assertThat(found.get().getState()).isEqualTo("state123");
    }

    @Test
    void findByState_whenStateDoesNotExist_thenReturnEmpty() {
        Optional<OAuth2Authorization> found = repository.findByState("nonExistentState");
        assertThat(found).isNotPresent();
    }

    @Test
    void findByAuthorizationCodeValue_whenCodeExists_thenReturnAuthorization() {
        Optional<OAuth2Authorization> found = repository.findByAuthorizationCodeValue("codeABC");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(auth1Id);
        assertThat(found.get().getAuthorizationCodeValue()).isEqualTo("codeABC");
    }

    @Test
    void findByAuthorizationCodeValue_whenCodeDoesNotExist_thenReturnEmpty() {
        Optional<OAuth2Authorization> found = repository.findByAuthorizationCodeValue("nonExistentCode");
        assertThat(found).isNotPresent();
    }

    @Test
    void findByAccessTokenValue_whenTokenExists_thenReturnAuthorization() {
        Optional<OAuth2Authorization> found = repository.findByAccessTokenValue("accessTokenXYZ");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(auth1Id);
        assertThat(found.get().getAccessTokenValue()).isEqualTo("accessTokenXYZ");
    }

    @Test
    void findByAccessTokenValue_whenTokenDoesNotExist_thenReturnEmpty() {
        Optional<OAuth2Authorization> found = repository.findByAccessTokenValue("nonExistentAccessToken");
        assertThat(found).isNotPresent();
    }

    @Test
    void findByRefreshTokenValue_whenTokenExists_thenReturnAuthorization() {
        Optional<OAuth2Authorization> found = repository.findByRefreshTokenValue("refreshToken123");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(auth1Id);
        assertThat(found.get().getRefreshTokenValue()).isEqualTo("refreshToken123");
    }

    @Test
    void findByRefreshTokenValue_whenTokenDoesNotExist_thenReturnEmpty() {
        Optional<OAuth2Authorization> found = repository.findByRefreshTokenValue("nonExistentRefreshToken");
        assertThat(found).isNotPresent();
    }

    @Test
    void findByUserCodeValue_whenCodeExists_thenReturnAuthorization() {
        Optional<OAuth2Authorization> found = repository.findByUserCodeValue("userCodeDEF");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(auth1Id);
        assertThat(found.get().getUserCodeValue()).isEqualTo("userCodeDEF");
    }

    @Test
    void findByUserCodeValue_whenCodeDoesNotExist_thenReturnEmpty() {
        Optional<OAuth2Authorization> found = repository.findByUserCodeValue("nonExistentUserCode");
        assertThat(found).isNotPresent();
    }

    @Test
    void findByDeviceCodeValue_whenCodeExists_thenReturnAuthorization() {
        Optional<OAuth2Authorization> found = repository.findByDeviceCodeValue("deviceCodeGHI");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(auth1Id);
        assertThat(found.get().getDeviceCodeValue()).isEqualTo("deviceCodeGHI");
    }

    @Test
    void findByDeviceCodeValue_whenCodeDoesNotExist_thenReturnEmpty() {
        Optional<OAuth2Authorization> found = repository.findByDeviceCodeValue("nonExistentDeviceCode");
        assertThat(found).isNotPresent();
    }

    // Tests for findByTokenValue
    @Test
    void findByTokenValue_whenTokenIsState_thenReturnAuthorization() {
        Optional<OAuth2Authorization> found = repository.findByTokenValue("state123");
        assertThat(found).isPresent().hasValueSatisfying(auth -> assertThat(auth.getId()).isEqualTo(auth1Id));
    }

    @Test
    void findByTokenValue_whenTokenIsAuthorizationCode_thenReturnAuthorization() {
        Optional<OAuth2Authorization> found = repository.findByTokenValue("codeABC");
        assertThat(found).isPresent().hasValueSatisfying(auth -> assertThat(auth.getId()).isEqualTo(auth1Id));
    }

    @Test
    void findByTokenValue_whenTokenIsAccessToken_thenReturnAuthorization() {
        Optional<OAuth2Authorization> found = repository.findByTokenValue("accessTokenXYZ");
        assertThat(found).isPresent().hasValueSatisfying(auth -> assertThat(auth.getId()).isEqualTo(auth1Id));
    }

    @Test
    void findByTokenValue_whenTokenIsRefreshToken_thenReturnAuthorization() {
        Optional<OAuth2Authorization> found = repository.findByTokenValue("refreshToken123");
        assertThat(found).isPresent().hasValueSatisfying(auth -> assertThat(auth.getId()).isEqualTo(auth1Id));
    }

    @Test
    void findByTokenValue_whenTokenIsUserCode_thenReturnAuthorization() {
        Optional<OAuth2Authorization> found = repository.findByTokenValue("userCodeDEF");
        assertThat(found).isPresent().hasValueSatisfying(auth -> assertThat(auth.getId()).isEqualTo(auth1Id));
    }

    @Test
    void findByTokenValue_whenTokenIsDeviceCode_thenReturnAuthorization() {
        Optional<OAuth2Authorization> found = repository.findByTokenValue("deviceCodeGHI");
        assertThat(found).isPresent().hasValueSatisfying(auth -> assertThat(auth.getId()).isEqualTo(auth1Id));
    }

    @Test
    void findByTokenValue_whenTokenDoesNotExistAnywhere_thenReturnEmpty() {
        Optional<OAuth2Authorization> found = repository.findByTokenValue("nonExistentTokenAnywhere");
        assertThat(found).isNotPresent();
    }

    @Test
    void findByTokenValue_whenTokenExistsInMultipleRecordsForDifferentFields_thenThrowsException() {
        // Setup: Create another auth record where one of its token fields matches a token field in auth1
        OAuth2Authorization auth3 = OAuth2Authorization.builder()
                .id(UUID.randomUUID().toString())
                .principalName("user3@example.com")
                .registeredClientId("client-id-3")
                .authorizationGrantType("password")
                .accessTokenValue("state123") // auth3.accessTokenValue = auth1.state
                .build();
        entityManager.persist(auth3);
        entityManager.flush();

        // Expect IncorrectResultSizeDataAccessException because "state123" will match auth1.state and auth3.accessTokenValue
        assertThatThrownBy(() -> repository.findByTokenValue("state123"))
                .isInstanceOf(IncorrectResultSizeDataAccessException.class);
    }

    // Tests for findByTokenValueAndTokenType
    @Test
    void findByTokenValueAndTokenType_state_success() {
        Optional<OAuth2Authorization> found = repository.findByTokenValueAndTokenType("state123", "state");
        assertThat(found).isPresent().hasValueSatisfying(auth -> assertThat(auth.getId()).isEqualTo(auth1Id));
    }

    @Test
    void findByTokenValueAndTokenType_code_success() {
        Optional<OAuth2Authorization> found = repository.findByTokenValueAndTokenType("codeABC", "code");
        assertThat(found).isPresent().hasValueSatisfying(auth -> assertThat(auth.getId()).isEqualTo(auth1Id));
    }

    @Test
    void findByTokenValueAndTokenType_accessToken_success() {
        Optional<OAuth2Authorization> found = repository.findByTokenValueAndTokenType("accessTokenXYZ", "access_token");
        assertThat(found).isPresent().hasValueSatisfying(auth -> assertThat(auth.getId()).isEqualTo(auth1Id));
    }

    @Test
    void findByTokenValueAndTokenType_refreshToken_success() {
        Optional<OAuth2Authorization> found = repository.findByTokenValueAndTokenType("refreshToken123", "refresh_token");
        assertThat(found).isPresent().hasValueSatisfying(auth -> assertThat(auth.getId()).isEqualTo(auth1Id));
    }

    @Test
    void findByTokenValueAndTokenType_userCode_success() {
        Optional<OAuth2Authorization> found = repository.findByTokenValueAndTokenType("userCodeDEF", "user_code");
        assertThat(found).isPresent().hasValueSatisfying(auth -> assertThat(auth.getId()).isEqualTo(auth1Id));
    }

    @Test
    void findByTokenValueAndTokenType_deviceCode_success() {
        Optional<OAuth2Authorization> found = repository.findByTokenValueAndTokenType("deviceCodeGHI", "device_code");
        assertThat(found).isPresent().hasValueSatisfying(auth -> assertThat(auth.getId()).isEqualTo(auth1Id));
    }

    @Test
    void findByTokenValueAndTokenType_tokenExistsButTypeMismatches_thenReturnEmpty() {
        Optional<OAuth2Authorization> found = repository.findByTokenValueAndTokenType("state123", "code"); // Correct token, wrong type
        assertThat(found).isNotPresent();
    }

    @Test
    void findByTokenValueAndTokenType_tokenDoesNotExist_thenReturnEmpty() {
        Optional<OAuth2Authorization> found = repository.findByTokenValueAndTokenType("nonExistentToken", "state");
        assertThat(found).isNotPresent();
    }

    @Test
    void findByTokenValueAndTokenType_invalidTokenType_thenReturnEmpty() {
        Optional<OAuth2Authorization> found = repository.findByTokenValueAndTokenType("state123", "invalid_type");
        assertThat(found).isNotPresent();
    }

    // Tests for deleteByPrincipalName (derived query)
    @Test
    void deleteByPrincipalName_whenPrincipalNameExists_thenDeletesAuthorizations() {
        OAuth2Authorization authUser1Another = OAuth2Authorization.builder()
                .id(UUID.randomUUID().toString())
                .principalName("user1@example.com")
                .registeredClientId("client-id-1")
                .authorizationGrantType("refresh_token")
                .state("anotherStateForUser1")
                .build();
        entityManager.persist(authUser1Another);
        entityManager.flush();

        long countUser1Before = repository.findAll().stream().filter(a -> "user1@example.com".equals(a.getPrincipalName())).count();
        assertThat(countUser1Before).isEqualTo(2); // auth1 and authUser1Another

        repository.deleteByPrincipalName("user1@example.com");
        entityManager.flush();
        entityManager.clear(); // Clear persistence context to ensure fresh read

        long countUser1After = repository.findAll().stream().filter(a -> "user1@example.com".equals(a.getPrincipalName())).count();
        assertThat(countUser1After).isZero();
        assertThat(repository.findById(auth2Id)).isPresent(); // Ensure user2's auth is not deleted
    }

    @Test
    void deleteByPrincipalName_whenPrincipalNameDoesNotExist_thenNoChange() {
        List<OAuth2Authorization> allBefore = repository.findAll();
        repository.deleteByPrincipalName("nonExistentUser@example.com");
        entityManager.flush();
        entityManager.clear();
        List<OAuth2Authorization> allAfter = repository.findAll();
        assertThat(allAfter).hasSameSizeAs(allBefore);
    }

    // Tests for deleteAllByPrincipalName (@Query with @Modifying)
    @Test
    void deleteAllByPrincipalName_whenPrincipalNameExists_thenDeletesAuthorizations() {
        OAuth2Authorization authUser1Another = OAuth2Authorization.builder()
                .id(UUID.randomUUID().toString())
                .principalName("user1@example.com")
                .registeredClientId("client-id-1")
                .authorizationGrantType("refresh_token")
                .state("yetAnotherStateForUser1")
                .build();
        entityManager.persist(authUser1Another);
        entityManager.flush();

        long countUser1Before = repository.findAll().stream().filter(a -> "user1@example.com".equals(a.getPrincipalName())).count();
        assertThat(countUser1Before).isEqualTo(2);

        repository.deleteAllByPrincipalName("user1@example.com");
        entityManager.flush(); // Crucial for @Modifying queries to hit the DB
        entityManager.clear(); // Crucial to reflect changes from bulk operations

        long countUser1After = repository.findAll().stream().filter(a -> "user1@example.com".equals(a.getPrincipalName())).count();
        assertThat(countUser1After).isZero();
        assertThat(repository.findById(auth2Id)).isPresent(); // Ensure user2's auth is not deleted
    }

    @Test
    void deleteAllByPrincipalName_whenPrincipalNameDoesNotExist_thenNoChange() {
        List<OAuth2Authorization> allBefore = repository.findAll();
        repository.deleteAllByPrincipalName("nonExistentUser@example.com");
        entityManager.flush();
        entityManager.clear();
        List<OAuth2Authorization> allAfter = repository.findAll();
        assertThat(allAfter).hasSameSizeAs(allBefore);
    }
}
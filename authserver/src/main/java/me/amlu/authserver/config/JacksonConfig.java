/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.webauthn4j.converter.jackson.WebAuthnJSONModule;
import com.webauthn4j.converter.util.ObjectConverter;
import me.amlu.authserver.config.jackson.mixin.webauthn.CustomWebauthnMixins;
import me.amlu.authserver.user.model.User;
import me.amlu.authserver.user.model.UserMixin;
import me.amlu.authserver.user.model.vo.AccountStatus;
import me.amlu.authserver.user.model.vo.AccountStatusMixin;
import me.amlu.authserver.user.model.vo.EmailAddress;
import me.amlu.authserver.user.model.vo.EmailAddressMixin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.NonNull;
import org.springframework.security.jackson2.CoreJackson2Module;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import org.springframework.security.web.jackson2.WebJackson2Module;
import org.springframework.security.web.webauthn.api.*;

import org.springframework.security.web.webauthn.jackson.WebauthnJackson2Module;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * Configures the primary Jackson {@link ObjectMapper} for the application.
 * This ObjectMapper is customized with modules and features for:
 * <ul>
 *     <li>Java 8 Date/Time (JSR-310) and Optional (Jdk8Module)</li>
 *     <li>Spring Security core types, OAuth2 authorization server types, and WebAuthn API types</li>
 *     <li>webauthn4j library types</li>
 *     <li>Custom application-specific types (User, EmailAddress, AccountStatus) via Mixins</li>
 *     <li>Custom Mixins for Spring Security WebAuthn API types to ensure correct session serialization</li>
 * </ul>
 * It is marked as {@link Primary} to ensure it's the default ObjectMapper injected by Spring,
 * notably used by Spring MVC for request/response body serialization and by {@link SessionConfig}
 * for Redis session serialization.
 */
@Configuration
public class JacksonConfig implements BeanClassLoaderAware { // Implement BeanClassLoaderAware

    private static final Logger log = LoggerFactory.getLogger(JacksonConfig.class);
    private ClassLoader classLoader;
//    private final PolymorphicTypeValidator sessionPolymorphicTypeValidator;
//
//    public JacksonConfig(@Qualifier("customSessionPolymorphicTypeValidator") PolymorphicTypeValidator sessionPolymorphicTypeValidator) {
//        this.sessionPolymorphicTypeValidator = sessionPolymorphicTypeValidator;
//    }

    // Example: If you need to make configurations profile-specific
    // private final Environment environment;
    // public JacksonConfig(Environment environment) {
    //     this.environment = environment;
    // }

    @Override
    public void setBeanClassLoader(@NonNull ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Creates and configures the primary {@link ObjectMapper} bean.
     * <p>
     * Key configurations include:
     * <ul>
     *   <li><b>Pretty Printing (INDENT_OUTPUT):</b> Enabled. Useful for development and debugging.
     *       For high-performance production environments, consider disabling or making it profile-specific.</li>
     *   <li><b>Date/Time Handling:</b> {@link JavaTimeModule} for JSR-310 types (e.g., {@code LocalDate})
     *       and {@link Jdk8Module} for {@code Optional}. Dates are serialized as ISO-8601 strings.</li>
     *   <li><b>Spring Security Modules:</b> Registers modules from {@link SecurityJackson2Modules}
     *       (covering core security, WebAuthn) and {@link OAuth2AuthorizationServerJackson2Module}.
     *       This is critical for serializing security-related objects in sessions or API responses.</li>
     *   <li><b>WebAuthn4j Module:</b> Registers {@link WebAuthnJSONModule} for types from the
     *       {@code com.webauthn4j} library. This complements Spring Security's WebAuthn support,
     *       especially if webauthn4j types are used directly or are part of complex objects.</li>
     *   <li><b>Custom Mixins:</b>
     *     <ul>
     *       <li>For application domain models ({@code User}, {@code EmailAddress}, {@code AccountStatus})
     *           to control their serialization/deserialization.</li>
     *       <li>For Spring Security WebAuthn API types (via {@link CustomWebauthnMixins}). These are
     *           essential for robust serialization of WebAuthn objects (e.g.,
     *           {@code PublicKeyCredentialCreationOptions}) when stored in the HTTP session,
     *           addressing potential issues with default serializers.</li>
     *     </ul>
     *   </li>
     *   <li><b>Serialization Inclusions:</b> {@code JsonInclude.Include.NON_EMPTY} to exclude null or empty
     *       values from output, reducing payload size.</li>
     *   <li><b>Deserialization Features:</b>
     *     <ul>
     *       <li>Strict parsing enabled for several features (e.g., {@code FAIL_ON_NULL_FOR_PRIMITIVES},
     *           {@code FAIL_ON_UNKNOWN_PROPERTIES}).</li>
     *       <li>{@code FAIL_ON_TRAILING_TOKENS} is set to {@code false} with a warning. This setting
     *           allows extra content after valid JSON, which can mask issues or pose security risks.
     *           It should be {@code true} unless there's a compelling, documented reason.</li>
     *     </ul>
     *   </li>
     *   <li><b>Default Typing:</b> Global default typing is NOT enabled due to security concerns.
     *       The mixin approach and type information on specific classes are preferred.</li>
     * </ul>
     * </p>
     *
     * @return The configured primary {@link ObjectMapper}.
     */
    @Bean
    @Primary // Mark this as the primary ObjectMapper
    public ObjectMapper objectMapper() {
        // Start with JsonMapper.builder() to correctly initialize and then configure
        JsonMapper.Builder builder = JsonMapper.builder()
                .disable(MapperFeature.DEFAULT_VIEW_INCLUSION); // Good practice

        // Add other builder configurations if needed before building
        // builder.addModule(new JavaTimeModule());
        // builder.addModule(new Jdk8Module());
        // List<Module> securityModules = SecurityJackson2Modules.getModules(this.classLoader);
        // builder.addModules(securityModules);
        // builder.addModule(new OAuth2AuthorizationServerJackson2Module());
        // ObjectConverter webauthn4jObjectConverter = new ObjectConverter();
        // builder.addModule(new WebAuthnJSONModule(webauthn4jObjectConverter));

        // Apply the injected PTV
        ObjectMapper objectMapperInstance = builder.build(); // Build the instance ONCE
//        objectMapper.setPolymorphicTypeValidator(this.sessionPolymorphicTypeValidator);
//        log.info("Applied injected PolymorphicTypeValidator to the primary ObjectMapper.");

        // --- Apply ALL configurations to this single objectMapperInstance ---

        // Apply the PolymorphicTypeValidator
        objectMapperInstance.setPolymorphicTypeValidator(customSessionPolymorphicTypeValidator());
        log.info("Applied customSessionPolymorphicTypeValidator to the primary ObjectMapper.");

        // --- Serialization Features ---
        // Enable pretty printing for easier debugging.
        // Note: In high-performance production environments, this might have a slight overhead.
        // Consider making this conditional on Spring profiles (e.g., active for 'dev', 'test').
        // if (environment.acceptsProfiles(Profiles.of("dev", "test"))) {
        //     objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        // }
        objectMapperInstance.enable(SerializationFeature.INDENT_OUTPUT);
        // Disable writing dates as timestamps, prefer ISO-8601 strings
        objectMapperInstance.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // Also configure for Durations explicitly
        objectMapperInstance.configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false);
        objectMapperInstance.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true); // For deterministic output
        objectMapperInstance.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        // Register the JavaTimeModule for Java 8 date/time API support
        objectMapperInstance.registerModule(new JavaTimeModule());
        log.debug("Registered JavaTimeModule for JSR-310 date/time types.");

        objectMapperInstance.registerModule(new Jdk8Module());
        log.debug("Registered Jdk8Module for JDK 8 types like Optional.");

        // --- Spring Security Modules ---
        // These are crucial for serializing/deserializing SecurityContext, Authentication, UserDetails,
        // and Spring Security's WebAuthn types (e.g., PublicKeyCredentialCreationOptions) if they are
        // stored in the session or transmitted over API.
        List<Module> securityModules = SecurityJackson2Modules.getModules(this.classLoader);
        objectMapperInstance.registerModules(securityModules);
        // This list should include WebauthnJackson2Module, which handles Bytes, PublicKeyCredentialType, etc.
        log.debug("Registered Spring Security Jackson modules (includes WebAuthn if on classpath): {}", securityModules);

        // Register Spring Security OAuth2 Authorization Server Jackson module
        objectMapperInstance.registerModule(new OAuth2AuthorizationServerJackson2Module());
        log.debug("Registered OAuth2AuthorizationServerJackson2Module for OAuth2-specific types.");

        // --- WebAuthn4j Module ---
        // Registers the Jackson module from the webauthn4j library.
        // This is useful if you're directly using webauthn4j types or if Spring Security types
        // internally use or expose webauthn4j types that need specific serialization.
        ObjectConverter webauthn4jObjectConverter = new ObjectConverter(); // webauthn4j's own converter
        objectMapperInstance.registerModule(new WebAuthnJSONModule(webauthn4jObjectConverter));
        log.debug("Registered WebAuthnJSONModule (from webauthn4j library).");

//        // Register Spring Security Jackson modules (redundant if SecurityJackson2Modules.getModules is used, but harmless)
//         objectMapperInstance.registerModule(new CoreJackson2Module());
//         objectMapperInstance.registerModule(new WebJackson2Module());

//        Explicitly registering WebauthnJackson2Module again is redundant if loaded by SecurityJackson2Modules,
        // but harmless. Kept for clarity.
        // Register Spring Security's WebAuthn Jackson module
        // This is critical for PublicKeyCredentialCreationOptions, etc.
//        objectMapperInstance.registerModule(new WebauthnJackson2Module());
//        log.debug("Registered WebauthnJackson2Module (from Spring Security).");


        // --- Register Mixin for custom User class ---
        // This tells Jackson that the User class is safe to deserialize and how to handle it.
        objectMapperInstance.addMixIn(User.class, UserMixin.class);
        log.debug("Registered mixin for application's User model.");
        objectMapperInstance.addMixIn(EmailAddress.class, EmailAddressMixin.class);
        log.debug("Registered mixin for application's EmailAddress VO.");
        objectMapperInstance.addMixIn(AccountStatus.class, AccountStatusMixin.class);
        log.debug("Registered mixin for application's AccountStatus VO.");

        // --- Custom WebAuthn Mixins for Spring Security API Types ---
        // These mixins are often necessary for robust serialization/deserialization of Spring Security's
        // WebAuthn API objects (e.g., PublicKeyCredentialCreationOptions, PublicKeyCredentialUserEntity)
        // when they are stored in the HTTP session. Default serializers might not always handle
        // the complexities of these objects correctly for session purposes.

        // Keep these if WebauthnJackson2Module doesn't fully handle session serialization/deserialization
        // for these specific types. The previous errors didn't point to these, but they were in your original code.
        // If you encounter issues with specific WebAuthn API types in the session, uncomment and implement their mixins.
        // For now, let's assume WebauthnJackson2Module is sufficient for these.
        // log.debug("Registering custom mixins for Spring Security WebAuthn API types (MyWebauthnMixins)...");
        // objectMapperInstance.addMixIn(PublicKeyCredentialCreationOptions.class, CustomWebauthnMixins.PublicKeyCredentialCreationOptionsMixIn.class);
        // ... add other WebAuthn API mixins if necessary ...
        // log.debug("Finished registering custom WebAuthn API mixins.");

        log.debug("Registering custom mixins for Spring Security WebAuthn API types (MyWebauthnMixins)...");
//        objectMapperInstance.addMixIn(PublicKeyCredentialCreationOptions.class, CustomWebauthnMixins.PublicKeyCredentialCreationOptionsMixIn.class);
//        objectMapperInstance.addMixIn(ImmutablePublicKeyCredentialUserEntity.class, CustomWebauthnMixins.PublicKeyCredentialUserEntityMixIn.class); // For the immutable variant
//        objectMapperInstance.addMixIn(PublicKeyCredentialUserEntity.class, CustomWebauthnMixins.PublicKeyCredentialUserEntityMixIn.class); // And the interface
//        objectMapperInstance.addMixIn(PublicKeyCredentialRpEntity.class, CustomWebauthnMixins.PublicKeyCredentialRpEntityMixIn.class);
//        objectMapperInstance.addMixIn(PublicKeyCredentialParameters.class, CustomWebauthnMixins.PublicKeyCredentialParametersMixIn.class);
//          objectMapperInstance.addMixIn(PublicKeyCredentialType.class, PublicKeyCredentialTypeMixIn.class);
//          objectMapperInstance.addMixIn(COSEAlgorithmIdentifier.class, COSEAlgorithmIdentifierMixIn.class);
//        objectMapperInstance.addMixIn(AuthenticatorSelectionCriteria.class, CustomWebauthnMixins.AuthenticatorSelectionCriteriaMixIn.class);
//          objectMapperInstance.addMixIn(AttestationConveyancePreference.class, AttestationConveyancePreferenceMixIn.class);
//          objectMapperInstance.addMixIn(AuthenticatorAttachment.class, AuthenticatorAttachmentMixIn.class);
//          objectMapperInstance.addMixIn(ResidentKeyRequirement.class, ResidentKeyRequirementMixIn.class);
//          objectMapperInstance.addMixIn(UserVerificationRequirement.class, UserVerificationRequirementMixIn.class);

//        objectMapperInstance.addMixIn(PublicKeyCredentialRequestOptions.class, CustomWebauthnMixins.PublicKeyCredentialRequestOptionsMixIn.class);
//        objectMapperInstance.addMixIn(ImmutableAuthenticationExtensionsClientInputs.class, CustomWebauthnMixins.AuthenticationExtensionsClientInputsMixIn.class); // For immutable variant
//        objectMapperInstance.addMixIn(AuthenticationExtensionsClientInputs.class, CustomWebauthnMixins.AuthenticationExtensionsClientInputsMixIn.class); // And the interface
//        objectMapperInstance.addMixIn(AuthenticationExtensionsClientInput.class, CustomWebauthnMixins.AuthenticationExtensionsClientInputMixIn.class);
//        objectMapperInstance.addMixIn(PublicKeyCredentialDescriptor.class, CustomWebauthnMixins.PublicKeyCredentialDescriptorMixIn.class);
//          objectMapperInstance.addMixIn(AuthenticatorTransport.class, AuthenticatorTransportMixIn.class);
//        objectMapperInstance.addMixIn(CredProtectAuthenticationExtensionsClientInput.class, CustomWebauthnMixins.CredProtectAuthenticationExtensionsClientInputMixIn.class);
//        objectMapperInstance.addMixIn(CredProtectAuthenticationExtensionsClientInput.CredProtect.class, CustomWebauthnMixins.CredProtectMixIn.class);
//        objectMapperInstance.addMixIn(Bytes.class, CustomWebauthnMixins.BytesMixIn.class);

        // The commented-out mixins from ObjectMapperFactory are for enums/simple types that
        // Spring Security's WebauthnJackson2Module should already handle.
        // If issues arise with these specific types, they can be uncommented and their mixins implemented.
        // e.g., objectMapperInstance.addMixIn(AuthenticatorTransport.class, MyWebauthnMixins.AuthenticatorTransportMixIn.class);
        log.debug("Finished registering custom WebAuthn API mixins.");
        log.info("Ensuring no other custom mixins for Spring Security WebAuthn API types are active, relying on WebauthnJackson2Module.");



        // --- General ObjectMapper Configurations ---
        // Configure ObjectMapper with necessary modules for ClientSettings and TokenSettings
//        ClassLoader classLoader = JacksonConfig.class.getClassLoader();
//        List<Module> securityModules = org.springframework.security.jackson2.SecurityJackson2Modules.getModules(classLoader);
//        objectMapperInstance.registerModules(securityModules);
//        objectMapperInstance.registerModule(new OAuth2AuthorizationServerJackson2Module());

        // IMPORTANT: Default Typing is a security risk if deserializing untrusted JSON.
        // For session serialization where you control the data, it can be acceptable
        // if strictly necessary, and other mechanisms (like specific mixins) are too complex.
        // If you enable it, use a PolymorphicTypeValidator to restrict allowed types.
        // Example (use with extreme caution and understand security implications):
        //
        // ObjectMapper.DefaultTypeResolverBuilder typer = new ObjectMapper.DefaultTypeResolverBuilder(
        //         ObjectMapper.DefaultTyping.NON_FINAL, objectMapper.getPolymorphicTypeValidator()
        // );
        // typer = typer.init(JsonTypeInfo.Id.CLASS, null);
        // typer = typer.inclusion(JsonTypeInfo.As.PROPERTY);
        // typer = typer.typeProperty("@class"); // Default property name
        // objectMapperInstance.setDefaultTyping(typer);
        // log.warn("Default typing enabled for ObjectMapper. Ensure this is secure for your use case.");

        objectMapperInstance.setSerializationInclusion(JsonInclude.Include.NON_EMPTY); // Exclude nulls and empty collections/strings
        objectMapperInstance.enable(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION); // Useful for debugging, includes source in JsonLocation

        // Explicitly disable default typing to prevent issues like "Type id handling not implemented"
        objectMapperInstance.deactivateDefaultTyping();
        log.info("Explicitly deactivated default typing for the primary ObjectMapper.");

        objectMapperInstance.disable(MapperFeature.DEFAULT_VIEW_INCLUSION); // DEPRECATED
//        objectMapperInstance = JsonMapper.builder().disable(MapperFeature.DEFAULT_VIEW_INCLUSION).build();  // Good practice

        // Serialization Features
        objectMapperInstance.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false); // Common setting

        objectMapperInstance.configure(SerializationFeature.INDENT_OUTPUT, true);
        objectMapperInstance.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false); // Already disabled at the top
        objectMapperInstance.configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false);
        objectMapperInstance.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

        // --- Deserialization Features ---
        objectMapperInstance.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);
        objectMapperInstance.configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, true);
        objectMapperInstance.configure(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY, true);

        // Determines whether encountering unknown properties (ones for which there is no
        // field or setter in the POJO) should result in a JsonMappingException.
        // Setting to 'true' provides stricter parsing. For session data, this is often a good default.
        // Set to 'false' if you need to be more lenient with incoming JSON (e.g., from external APIs).
        objectMapperInstance.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        log.debug("Configured DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES to true (strict).");

        // Determines whether encountering properties that have been explicitly marked as ignorable
        // (e.g., via @JsonIgnore) should result in an exception.
        // Default is 'false', meaning they are silently ignored if encountered. 'true' is very strict.
        objectMapperInstance.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false); // Changed from true to false (default and common)
        log.debug("Configured DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES to false (default behavior).");


        objectMapperInstance.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false); // Allows flexibility with @JsonCreator

        // --- MODIFICATION AS PER INPUT ---
        // objectMapperInstance.configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, true); // Original, generally recommended for strictness

        // TODO: [SECURITY][IMPORTANT] Review the necessity of FAIL_ON_TRAILING_TOKENS = false.
        // This setting allows Jackson to ignore any characters/tokens that appear after a valid JSON object/array
        // has been parsed from the input stream.
        // Potential Risks:
        // 1. Masking Malformed JSON: It might hide issues with data sources that produce non-compliant JSON.
        // 2. Security Vulnerabilities: If an attacker can append data to a legitimate JSON payload,
        //    and this appended data is ignored by Jackson but processed by a subsequent component
        //    (e.g., if the raw input stream is read again), it could lead to vulnerabilities like
        //    HTTP Parameter Pollution or other injection attacks.
        // Recommendation:
        // - Revert to 'true' if possible for stricter and safer parsing.
        // - If 'false' is absolutely required (e.g., due to an external system sending trailing newlines
        //   that cannot be fixed at the source), this decision and its mitigation strategies
        //   (e.g., ensuring no downstream processing of the raw input) must be thoroughly documented.
        objectMapperInstance.configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, false); // Changed to false as per input
        log.warn("DeserializationFeature.FAIL_ON_TRAILING_TOKENS has been set to false. " +
                "This allows Jackson to ignore extra content after the main JSON document. " +
                "This can mask malformed JSON and may have security implications. " +
                "Ensure this is intentional, thoroughly understood, and documented. See TODO in code.");
        // --- END MODIFICATION ---

        objectMapperInstance.configure(DeserializationFeature.FAIL_ON_UNEXPECTED_VIEW_PROPERTIES, true); // If using @JsonView
        objectMapperInstance.configure(DeserializationFeature.ACCEPT_FLOAT_AS_INT, false); // Prevents precision loss

        // --- Default Typing (Security Note) ---
        // Global default typing (objectMapperInstance.activateDefaultTyping(...)) is generally NOT recommended
        // due to security vulnerabilities (potential for remote code execution if deserializing
        // untrusted JSON containing malicious type information).
        // The current approach of using specific modules, mixins, and @JsonTypeInfo on
        // targeted classes (where necessary and with controlled subtypes) is much safer.
        // The commented-out block below should remain commented unless security implications are fully understood
        // and mitigated with a very restrictive PolymorphicTypeValidator.
        /*
        if (overrideDefaultTypingFromDefaultJacksonSecurityModules) { // This would be a boolean property if needed
            log.warn("Activating global default typing. Ensure this is secure and necessary.");
            objectMapperInstance.activateDefaultTyping(
                    LaissezFaireSubTypeValidator.instance, // DANGEROUS: Or a more restrictive custom validator
                    ObjectMapper.DefaultTyping.NON_FINAL,
                    JsonTypeInfo.As.PROPERTY // Example configuration
            );
        }
        */

        log.info("Primary ObjectMapper configured with: JSR310, Jdk8, Spring Security (Core, OAuth2, WebAuthn), " +
                "WebAuthn4j, custom application mixins, and custom WebAuthn API mixins for session serialization.");
        return objectMapperInstance;
    }

    @Bean
    public PolymorphicTypeValidator customSessionPolymorphicTypeValidator() {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                // Allow common Java utility types
                .allowIfBaseType(java.util.Collection.class)
                .allowIfBaseType(java.util.Map.class)
                .allowIfSubType(java.util.ArrayList.class) // Specific common collections
                .allowIfSubType(java.util.LinkedList.class)
                .allowIfSubType(java.util.HashSet.class)
                .allowIfSubType(java.util.LinkedHashMap.class)
                .allowIfSubType(java.util.HashMap.class)
                .allowIfSubType(java.util.Date.class) // If you use java.util.Date
                .allowIfSubType(java.time.Instant.class) // For Java Time API

                // Allow Spring Security core types
                .allowIfSubType("org.springframework.security.core.")
                .allowIfSubType("org.springframework.security.authentication.")
                .allowIfSubType("org.springframework.security.web.authentication.WebAuthenticationDetails")
                .allowIfSubType("org.springframework.security.web.csrf.") // For CsrfToken
                .allowIfSubType("org.springframework.security.oauth2.core.") // For OAuth2 types
                .allowIfSubType("org.springframework.security.oauth2.client.") // For OAuth2 client types

                .allowIfSubType("org.springframework.security.web.savedrequest.") // Allows DefaultSavedRequest and others in this package
                // Alternatively, for more specificity:
                // .allowIfSubType(DefaultSavedRequest.class)

                .allowIfSubType("org.springframework.security.oauth2.core.")
                .allowIfSubType("org.springframework.security.oauth2.client.")
                // Allow Spring Security WebAuthn types (if stored in session, e.g., options)
                .allowIfSubType("org.springframework.security.web.webauthn.api.")


                // Allow your application's domain/model classes that might be in the session
                .allowIfSubType("me.amlu.authserver.user.model.") // For your User, EmailAddress, etc.
                .allowIfSubType("me.amlu.authserver.user.model.vo.")
                .allowIfSubType("me.amlu.authserver.oauth2.model.") // For Authority, etc.
                .allowIfSubType("me.amlu.authserver.passkey.model.") // If PasskeyCredential or related are in session

                // Add any other packages or specific classes you store in the session
                // For extreme debugging, you could temporarily use:
                // .allowIfSubType(Object.class) // VERY PERMISSIVE - NOT FOR PRODUCTION
                .build();
        log.info("Configured customSessionPolymorphicTypeValidator: {}", ptv);
        return ptv;
    }

    // --- Mixin for Spring Security's Bytes class ---
    // This ensures Bytes.toBase64Url() is used for serialization.
    abstract static class SpringBytesMixin {
        @JsonValue
        public abstract String toBase64Url();
    }

    // --- Mixin for Spring Security's enum-like wrapper classes ---
    // This ensures that for classes like PublicKeyCredentialType, AttestationConveyancePreference, etc.,
    // their .getValue() method (which returns the simple string) is used for serialization.
    abstract static class SpringWebAuthnEnumWrapperMixin {
        @JsonValue
        public abstract String getValue();
    }

    // --- Mixin for COSEAlgorithmIdentifier ---
    // This ensures its numeric long value is used.
    abstract static class SpringCOSEAlgorithmIdentifierMixin {
        @JsonValue
        public abstract Long getValue();
    }


    @Bean
    @Qualifier("webAuthnApiMapper")
    public ObjectMapper webAuthnApiMapper() {
        ObjectMapper mapper = JsonMapper.builder()
                .disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
                .build();

        // 1. JavaTimeModule with custom Duration serializer for milliseconds
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(Duration.class, new JsonSerializer<Duration>() {
            @Override
            public void serialize(Duration duration, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                if (duration == null) {
                    gen.writeNull();
                } else {
                    gen.writeNumber(duration.toMillis()); // Serialize Duration to milliseconds
                }
            }
        });
        mapper.registerModule(javaTimeModule);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // Keep dates as ISO strings

        // 2. Spring Security's WebAuthn module (should handle most API objects)
        mapper.registerModule(new WebauthnJackson2Module());
        log.info("webAuthnApiMapper: Registered WebauthnJackson2Module.");

        // 3. Explicit Mixins to ensure plain values for client-side WebAuthn API,
        //    overriding any verbose serialization that WebauthnJackson2Module might still produce
        //    for these specific types when not used with default typing.

        // 3. Explicit Custom Serializer for Bytes.class to ensure plain string output
        //    This will take precedence for serializing any object that is an instance of Bytes.
        JsonSerializer<Bytes> explicitBytesSerializer = new JsonSerializer<Bytes>() {
            @Override
            public void serialize(Bytes value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                if (value == null) {
                    gen.writeNull();
                } else {
                    gen.writeString(value.toBase64UrlString()); // Use the interface method
                }
            }
        };
        SimpleModule explicitBytesHandlingModule = new SimpleModule("ExplicitBytesHandlingModule");
        explicitBytesHandlingModule.addSerializer(Bytes.class, explicitBytesSerializer);
        mapper.registerModule(explicitBytesHandlingModule);
        log.info("Registered ExplicitBytesHandlingModule with direct Bytes.class serializer on webAuthnApiMapper.");

        // Remove previous mixin attempts for Bytes to avoid conflicts with the explicit serializer
        // mapper.removeMixIn(Bytes.class); // If addMixIn was used before, ensure it's not active for Bytes.class
        // mapper.removeMixIn(Base64EncodedBytes.class); // And for any concrete types
//        mapper.addMixIn(Bytes.class, SpringBytesMixin.class);
//        mapper.addMixIn(PublicKeyCredentialCreationOptions.class, SpringBytesMixin.class); // Concrete class used by Spring Security

        mapper.addMixIn(PublicKeyCredentialType.class, SpringWebAuthnEnumWrapperMixin.class);
        mapper.addMixIn(AttestationConveyancePreference.class, SpringWebAuthnEnumWrapperMixin.class);
        mapper.addMixIn(AuthenticatorAttachment.class, SpringWebAuthnEnumWrapperMixin.class); // If it's a wrapper with getValue()
        mapper.addMixIn(UserVerificationRequirement.class, SpringWebAuthnEnumWrapperMixin.class);
        mapper.addMixIn(ResidentKeyRequirement.class, SpringWebAuthnEnumWrapperMixin.class);
        mapper.addMixIn(COSEAlgorithmIdentifier.class, SpringCOSEAlgorithmIdentifierMixin.class);


        // 4. General settings for client-facing JSON
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL); // Exclude null fields
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES); // Be lenient on input if ever used for deserialization
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        // 5. Ensure NO default typing is active for this mapper
        mapper.deactivateDefaultTyping();

//        log.info("Configured 'webAuthnApiMapper' for client-side WebAuthn JSON serialization.");
        log.info("Configured 'webAuthnApiMapper' with explicit Bytes serializer for plain string output.");
        return mapper;
    }
}

/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */


package me.amlu.authserver.security.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import me.amlu.authserver.exceptions.EncryptionException;
import me.amlu.authserver.security.service.EncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * JPA AttributeConverter to automatically encrypt and decrypt byte array data
 * when persisting to and loading from the database.
 * <p>
 * This converter uses an {@link EncryptionService} to perform the actual encryption/decryption.
 * It is designed to be used with specific entity attributes by applying the
 * {@code @Convert(converter = EncryptedByteArrayConverter.class)} annotation.
 * <p>
 * Due to how JPA instantiates converters, a static field and setter injection pattern
 * is used to make the Spring-managed {@link EncryptionService} available to JPA-managed
 * converter instances.
 */
@Converter(autoApply = false) // Set autoApply=false initially. Apply explicitly where needed.
@Component // Make it a Spring component
public class EncryptedByteArrayConverter implements AttributeConverter<byte[], byte[]> {

    private static final Logger log = LoggerFactory.getLogger(EncryptedByteArrayConverter.class);

    // Use a static field to hold the injected service.
    // This is a common pattern for JPA AttributeConverters that need Spring beans.
    private static EncryptionService staticEncryptionService;


    /*
    Important Notes on the Static Injection Pattern for Converters: JPA AttributeConverter instances are typically instantiated by the JPA provider directly, not by Spring's ApplicationContext.
    This means direct @Autowired on fields or constructor injection doesn't work as expected for the instances JPA uses. The static field + setter injection pattern is a common workaround:
    1.The converter is declared as a @Component so Spring creates an instance and injects the EncryptionService into the static field via the setter.
    2.When JPA instantiates its own instances of the converter, they can access the service through this static field.
    3.Set autoApply = false on @Converter initially. You'll explicitly apply it to fields in your PasskeyCredential entity using @Convert(converter = EncryptedByteArrayConverter.class).
    If you set autoApply = true, ensure this converter is appropriate for all byte[] attributes in your persistence unit.
     */
    // Inject via setter

    /**
     * Sets the static {@link EncryptionService} instance. This method is called by Spring
     * when the {@link EncryptedByteArrayConverter} component is initialized.
     *
     * @param encryptionService The {@link EncryptionService} to be used for encryption/decryption.
     */
    @Autowired
    public void setEncryptionService(EncryptionService encryptionService) {
        if (EncryptedByteArrayConverter.staticEncryptionService == null) {
            log.info("Setting static EncryptionService in EncryptedByteArrayConverter");
            EncryptedByteArrayConverter.staticEncryptionService = encryptionService;
        } else {
            // This log indicates it's being called again while already set.
            log.warn("Static EncryptionService in EncryptedByteArrayConverter already set. Current: {}, New: {}. This might indicate multiple initializations or context reloads.",
                    System.identityHashCode(EncryptedByteArrayConverter.staticEncryptionService),
                    System.identityHashCode(encryptionService));
            // Optionally, only set if the instance is different, though with singletons this shouldn't happen.
            // if (EncryptedByteArrayConverter.staticEncryptionService != encryptionService) {
            //    log.warn("Re-setting static EncryptionService with a different instance. This is unusual.");
            //    EncryptedByteArrayConverter.staticEncryptionService = encryptionService;
            // }
        }
    }

    // No-arg constructor for JPA and Spring (if Spring creates it as a bean)

    /**
     * Constructs an instance of {@link EncryptedByteArrayConverter}.
     * This constructor is required by JPA and also used by Spring when creating the component bean.
     */
    public EncryptedByteArrayConverter() {
        log.trace("EncryptedByteArrayConverter instance created.");
    }


    /**
     * Converts the entity attribute value (a byte array) into a database column value (an encrypted byte array).
     *
     * @param attribute The byte array from the entity attribute.
     * @return The encrypted byte array to be stored in the database column, or null if the input is null.
     */
    @Override
    public byte[] convertToDatabaseColumn(byte[] attribute) {
        if (attribute == null) {
            return null;
        }
        if (staticEncryptionService == null) {
            log.error("EncryptionService not injected into EncryptedByteArrayConverter. Cannot encrypt.");
            throw new IllegalStateException("EncryptionService not available for encryption.");
        }
        try {
            return staticEncryptionService.encrypt(attribute);
        } catch (EncryptionException e) {
            log.error("Failed to encrypt data for database column", e);
            // Decide on error handling: re-throw, return null, or return unencrypted?
            // Rethrowing is generally safer to prevent accidental storage of unencrypted data.
            throw e;
        }
    }

    /**
     * Converts the database column value (an encrypted byte array) into an entity attribute value (a decrypted byte array).
     *
     * @param dbData The encrypted byte array from the database column.
     * @return The decrypted byte array for the entity attribute, or null if the input is null.
     */
    @Override
    public byte[] convertToEntityAttribute(byte[] dbData) {
        if (dbData == null) {
            return null;
        }
        if (staticEncryptionService == null) {
            log.error("EncryptionService not injected into EncryptedByteArrayConverter. Cannot decrypt.");
            throw new IllegalStateException("EncryptionService not available for decryption.");
        }
        try {
            return staticEncryptionService.decrypt(dbData);
        } catch (EncryptionException e) {
            log.error("Failed to decrypt data from database column", e);
            // Decide on error handling: re-throw, return null, or corrupted data?
            // Rethrowing is common.
            throw e;
        }
    }
}

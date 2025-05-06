/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.config;

import org.springframework.context.annotation.Configuration;

/**
 * Configuration for encryption, specifically for the TextEncryptor used for MFA secrets.
 * <p>
 * This configuration attempts to retrieve the encryption password and salt from HashiCorp Vault
 * via Spring Cloud Vault.
 * If Vault is unavailable at startup (and fail-fast is false) or if
 * the specific secrets are not found in Vault, it falls back to using environment variables
 * defined by the properties `mfa.encryption.password` and `mfa.encryption.salt`.
 * <p>
 * Prerequisites:
 * 1. `spring-cloud-starter-vault-config` dependency must be included.
 * 2. Vault connection details (`spring.cloud.vault.uri`, `spring.cloud.vault.token`, etc.)
 * must be configured in `application.yml` or `bootstrap.yml`.
 * 3. `spring.cloud.vault.fail-fast` should be set to `false` in configuration to allow fallback.
 * 4. `spring.config.import` should include `optional:vault://` for graceful startup if Vault is unreachable.
 * 5. The secrets `mfa.encryption.password` and `mfa.encryption.salt` should exist in Vault
 * under the path configured for Spring Cloud Vault (e.g., `secret/amlume-shop/mfa`).
 * 6. Environment variables `MFA_ENCRYPTION_PASSWORD` and `MFA_ENCRYPTION_SALT` must be set
 * in the deployment environment to serve as the ultimate fallback.
 */
@Configuration
public class EncryptionConfig {

}
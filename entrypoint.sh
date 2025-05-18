#!/bin/sh
#
# Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
#
# This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
#
# Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
#
# Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
#

# Exit immediately if a command exits with a non-zero status.
set -e

# --- Configuration (Read from environment variables) ---
VAULT_ADDR="${VAULT_ADDR:-http://vault:8200}"
VAULT_TOKEN="${VAULT_TOKEN}"
# BASE_VAULT_PATH_PREFIX is the logical path Spring Cloud Vault will read from
BASE_VAULT_PATH_PREFIX="${VAULT_PATH:-secret/amlume-shop/local}"
ENV_FILE_PATH="${ENV_FILE_PATH:-/.env}"
VAULT_READY_TIMEOUT="${VAULT_READY_TIMEOUT:-60}"

# --- SECURITY WARNING ---
# Reading secrets directly from an .env file (ENV_FILE_PATH) is NOT recommended for production.
# This file might be exposed if the container image is compromised or improperly handled.
# Consider using Vault Agent sidecar injection or Kubernetes secrets mounted as volumes
# to inject secrets directly into the application container or make them available securely.
# --- END SECURITY WARNING ---

# --- SECURITY WARNING ---
# Passing a long-lived VAULT_TOKEN with write permissions to this script is risky.
# If this token is compromised, an attacker could modify secrets in Vault.
# Consider using short-lived tokens, Vault Agent with AppRole/K8s Auth, or other
# secure authentication methods for Vault instead of passing a static token.
# --- END SECURITY WARNING ---


echo "Seeder: Starting up (Single Path Mode)."
echo "Seeder: Target Vault Addr: ${VAULT_ADDR}"
echo "Seeder: Target Vault Path (Logical): $BASE_VAULT_PATH_PREFIX"
echo "Seeder: Source Env File: $ENV_FILE_PATH"
echo "Seeder: Using Vault Token: $(echo "$VAULT_TOKEN" | head -c 5)... (masked)"

if [ -z "$VAULT_TOKEN" ]; then
  echo "Seeder: Error - VAULT_TOKEN environment variable is not set."
  exit 1
fi

if [ ! -f "$ENV_FILE_PATH" ]; then
    echo "Seeder: Error - Env file not found at $ENV_FILE_PATH"
    exit 1
fi

# --- Wait for Vault to be Ready ---
echo "Seeder: Waiting for Vault to become ready at $VAULT_ADDR..."
start_time=$(date +%s)
while true; do
  if curl --output /dev/null --silent --head --fail "$VAULT_ADDR/v1/sys/health"; then
    echo "Seeder: Vault is initialized, unsealed, and active."
    break
  fi
  current_time=$(date +%s)
  elapsed_time=$((current_time - start_time))
  if [ "$elapsed_time" -ge "$VAULT_READY_TIMEOUT" ]; then
    echo "Seeder: Error - Timeout waiting for Vault to become ready after $VAULT_READY_TIMEOUT seconds."
    curl --silent --show-error "$VAULT_ADDR/v1/sys/health" || echo "Seeder: Could not reach Vault health endpoint."
    exit 1
  fi
  echo "Seeder: Vault not ready yet, sleeping for 3 seconds..."
  sleep 3
done

# --- Source the .env file ---
echo "Seeder: Sourcing environment variables from $ENV_FILE_PATH..."
# ... (File existence and readability checks remain the same) ...
ls -l "$ENV_FILE_PATH" || { echo "Seeder: Error - Env file not found at $ENV_FILE_PATH"; exit 1; }
if [ ! -r "$ENV_FILE_PATH" ]; then
    echo "Seeder: Error - Env file is not readable at $ENV_FILE_PATH"; exit 1;
fi
set -a
# shellcheck source=/dev/null
. "$ENV_FILE_PATH"
set +a
echo "Seeder: Environment variables sourced."

# --- Construct the SINGLE JSON Payload ---
echo "Seeder: Constructing JSON payload for Vault..."

# NOTE: Using simple string concatenation. For complex values or robustness,
# consider installing and using 'jq' if available in your container image.
# Example with jq:
# JSON_PAYLOAD=$(jq -n \
#   --arg cors_origins "$CORS_ALLOWED_ORIGINS" \
#   --arg db_pass "$DB_PASSWORD" \
#   ... \
#   '{data: {
#       "cors.allowed-origins": $cors_origins,
#       "database.password": $db_pass,
#       ...
#    }}')

JSON_DATA_PAIRS=""
add_pair() {
    key="$1"
    value="$2"
    # Escape backslashes, then double quotes, then newlines
    escaped_value=$(echo "$value" | sed -e ':a' -e 'N' -e '$!ba' -e 's/\\/\\\\/g; s/"/\\"/g; s/\n/\\n/g')

    if [ -z "$JSON_DATA_PAIRS" ]; then
        JSON_DATA_PAIRS="\"$key\":\"$escaped_value\""
    else
        JSON_DATA_PAIRS="$JSON_DATA_PAIRS,\"$key\":\"$escaped_value\""
    fi
}

# --- Add ALL secrets with correct Spring property names (dot-notation) ---
# Match these keys with your @ConfigurationProperties beans

add_pair "app.ssl.trust-store.password" "${APP_CENTRAL_TRUSTSTORE_PASSWORD}"
add_pair "cors.allowed-origins" "${CORS_ALLOWED_ORIGINS}"

add_pair "database.password" "${SHOP_DB_PASSWORD}"
add_pair "database.username" "${SHOP_DB_USER}"
# Add other database.* properties if needed (e.g., database.url if not hardcoded)

add_pair "device-fingerprint.salt" "${DEVICE_FINGERPRINT_SALT}" # Assuming property name matches

# Email properties (match keys in application-local.yml if loaded via @ConfigurationProperties)
# If using spring.mail.* directly, these might not be needed in Vault unless overridden
add_pair "email.team-email" "${TEAM_EMAIL}"
add_pair "email.alert-email-sender" "${ALERT_EMAIL_SENDER}"
add_pair "email.notification-email-to" "${NOTIFICATION_EMAIL_TO}"
add_pair "email.notification-email-from" "${NOTIFICATION_EMAIL_FROM}"
# Spring Mail properties (if you want them in Vault)
add_pair "spring.mail.host" "${MAIL_HOST}"
add_pair "spring.mail.username" "${MAIL_USERNAME}"
add_pair "spring.mail.password" "${MAIL_PASSWORD}"
add_pair "spring.mail.port" "${MAIL_PORT}"
add_pair "spring.mail.properties.mail.smtp.starttls.enable" "${MAIL_STARTTLS_ENABLE}"
add_pair "spring.mail.properties.mail.smtp.auth" "${MAIL_AUTH_ENABLE}"

add_pair "geoip2.account-id" "${GEOIP2_ACCOUNT_ID}"
add_pair "geoip2.license-key" "${GEOIP2_LICENSE_KEY}"
# Add other geoip2.* properties if needed (e.g., database paths if loaded from Vault)

add_pair "hashing.algorithm" "${HASHING_ALGORITHM}"
add_pair "hashing.iterations" "${HASHING_ITERATIONS}"
add_pair "hashing.key-length" "${HASHING_KEY_LENGTH}"
add_pair "hashing.encoding" "${HASH_ENCODING}"

# MFA properties (match MfaProperties bean)
add_pair "mfa.mfaEncryptionPassword" "${MFA_ENCRYPTION_PASSWORD}" # Check exact property name in MfaProperties
add_pair "mfaEncryptionPassword" "${MFA_ENCRYPTION_PASSWORD}" # Check exact property name in MfaProperties
add_pair "mfa.mfaEncryptionSalt" "${MFA_ENCRYPTION_SALT}"         # Check exact property name in MfaProperties
add_pair "mfaEncryptionSalt" "${MFA_ENCRYPTION_SALT}"         # Check exact property name in MfaProperties

add_pair "observability.loki-url" "${LOKI_URL}" # Assuming property name matches

# PASETO properties (match PasetoProperties bean structure)
add_pair "paseto.pub.access.private-key" "${PASETO_PUBLIC_ACCESS_PRIVATE_KEY}"
add_pair "paseto.pub.access.public-key" "${PASETO_PUBLIC_ACCESS_PUBLIC_KEY}"
add_pair "paseto.pub.access.kid" "${PASETO_PUBLIC_ACCESS_KID}"
# Add paseto.pub.access.expiration if needed

add_pair "paseto.local.access.secret-key" "${PASETO_LOCAL_ACCESS_SECRET_KEY}"
add_pair "paseto.local.access.kid" "${PASETO_LOCAL_ACCESS_KID}"
# Add paseto.local.access.expiration if needed

add_pair "paseto.local.refresh.secret-key" "${PASETO_LOCAL_REFRESH_SECRET_KEY}"
add_pair "paseto.local.refresh.kid" "${PASETO_LOCAL_REFRESH_KID}"
# Add paseto.local.refresh.expiration if needed

# Add paseto.pub.refresh.* if used
add_pair "paseto.pub.refresh.private-key" "${PASETO_PUBLIC_REFRESH_PRIVATE_KEY}"
add_pair "paseto.pub.refresh.public-key" "${PASETO_PUBLIC_REFRESH_PUBLIC_KEY}"
add_pair "paseto.pub.refresh.kid" "${PASETO_PUBLIC_REFRESH_KID}"
# Add paseto.pub.refresh.expiration if needed

# reCAPTCHA properties (match RecaptchaProperties bean or @Value keys)
add_pair "recaptcha.secret" "${RECAPTCHA_SECRET_KEY}" # Check exact property name used by Spring
add_pair "recaptcha.site-key" "${RECAPTCHA_SITE_KEY}" # Check exact property name used by Spring

# Slack properties (match SlackProperties bean or @Value keys)
add_pair "slack.bot-token" "${SLACK_BOT_TOKEN}"
add_pair "slack.channel" "${SLACK_CHANNEL}"
add_pair "slack.channel-name" "${SLACK_CHANNEL_NAME}"
add_pair "slack.api-token" "${SLACK_API_TOKEN}"
add_pair "slack.webhook-url" "${SLACK_WEBHOOK_URL}"
# Add other slack.* properties if needed

# Valkey properties (match ValkeyConfigProperties bean)
add_pair "valkey.password" "${VALKEY_PASSWORD}"
add_pair "valkey.host" "${VALKEY_HOST}"
add_pair "valkey.port" "${VALKEY_PORT}"
# Add valkey.pool.* properties if needed

# Whois properties (match WhoisProperties bean or @Value keys)
add_pair "web.whois.server" "${WHOIS_SERVER}" # Check exact property name used by Spring
add_pair "web.whois.port" "${WHOIS_PORT}"     # Check exact property name used by Spring
add_pair "web.whois.timeout" "${WHOIS_TIMEOUT}" # Check exact property name used by Spring

# --- Final JSON Payload ---
JSON_PAYLOAD="{\"data\":{$JSON_DATA_PAIRS}}"
# echo "DEBUG: Payload: $JSON_PAYLOAD" # Uncomment for debugging

# --- Write the SINGLE Payload to Vault ---
echo "Seeder: Writing all secrets to Vault path $BASE_VAULT_PATH_PREFIX..."

# Construct the correct KVv2 API path: <mount>/data/<logical_path>
VAULT_MOUNT=$(echo "$BASE_VAULT_PATH_PREFIX" | cut -d'/' -f1)
VAULT_LOGICAL_PATH=$(echo "$BASE_VAULT_PATH_PREFIX" | cut -d'/' -f2-)
API_PATH="${VAULT_MOUNT}/data/${VAULT_LOGICAL_PATH}" # Path to write the single secret object
API_URL="${VAULT_ADDR}/v1/${API_PATH}"

echo "Seeder: Writing via curl to API URL: $API_URL"

# Execute curl (using the same robust error handling as before)
http_status_output=$(curl -f -w "%{http_code}" \
     --request POST \
     --header "X-Vault-Token: ${VAULT_TOKEN}" \
     --header "Content-Type: application/json" \
     --data "$JSON_PAYLOAD" \
     "$API_URL" \
     --output /tmp/curl_response.txt \
     --stderr /tmp/curl_stderr.txt)
curl_exit_code=$? # Capture exit code IMMEDIATELY

# Check curl's exit code first
if [ $curl_exit_code -ne 0 ]; then
    echo "Seeder: Error writing secrets via curl to API path: $API_PATH (curl exit code: $curl_exit_code)" >&2
    if printf '%s' "$http_status_output" | grep -qE '^[1-5][0-9]{2}$'; then
         echo "Seeder: HTTP Status Code: $http_status_output" >&2
    else
         echo "Seeder: HTTP Status Code: Unknown (curl exit code $curl_exit_code indicated failure. Output was '$http_status_output')" >&2
    fi
    echo "--- Seeder: Response Body Start ---" >&2
    cat /tmp/curl_response.txt >&2 || echo "(No response body captured)" >&2
    echo "--- Seeder: Response Body End ---" >&2
    echo "--- Seeder: Stderr Start ---" >&2
    cat /tmp/curl_stderr.txt >&2 || echo "(No stderr captured)" >&2
    echo "--- Seeder: Stderr End ---" >&2
    rm -f /tmp/curl_response.txt /tmp/curl_stderr.txt
    exit 1 # Explicitly exit on error
else
    echo "Seeder: Successfully wrote secrets via curl to API path: $API_PATH (HTTP Status: $http_status_output)"
    rm -f /tmp/curl_response.txt /tmp/curl_stderr.txt
fi

echo "Seeder: Vault population script finished successfully."
exit 0

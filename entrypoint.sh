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

# Vault Seeder Script
# This script populates Vault with secrets from a .env file.
# It uses the Vault CLI to write secrets to a specified path in Vault.

# Exit immediately if a command exits with a non-zero status.
set -e

# --- Configuration (Read from environment variables) ---
VAULT_ADDR="${VAULT_ADDR:-http://vault:8200}"
VAULT_TOKEN="${VAULT_TOKEN}"
# VAULT_PATH is no longer used directly for the put command, but keep for base path logic if needed
BASE_VAULT_PATH="${VAULT_PATH:-secret/amlume-shop/local}"
ENV_FILE_PATH="${ENV_FILE_PATH:-/.env}"
VAULT_READY_TIMEOUT="${VAULT_READY_TIMEOUT:-60}"

echo "Seeder: Starting up."
echo "Seeder: Target Vault Addr: $VAULT_ADDR"
echo "Seeder: Target Base Vault Path: $BASE_VAULT_PATH" # Log base path
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
# ... (Vault readiness check remains the same) ...
echo "Seeder: Waiting for Vault to become ready at $VAULT_ADDR..."
start_time=$(date +%s)
while true; do
  if curl --fail --silent --show-error "$VAULT_ADDR/v1/sys/health" | jq -e '.initialized == true and .sealed == false and .standby == false' > /dev/null; then
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

# --- Source the .env file to load variables into the script's environment ---
echo "Seeder: Sourcing environment variables from $ENV_FILE_PATH..."
# Use 'set -a' to export all variables defined in the .env file
set -a
# shellcheck source=/dev/null # Tell shellcheck to ignore SC1090/SC1091 if needed
. "$ENV_FILE_PATH"
set +a
echo "Seeder: Environment variables sourced."

# --- Populate Vault with Specific Secrets ---
echo "Seeder: Populating specific secrets into Vault..."

# Export token for Vault CLI commands
export VAULT_TOKEN

# Function to handle vault put errors
vault_put() {
    path="$1"
    shift # Remove path from arguments
    echo "Seeder: Writing to Vault path: $path"
    # Use "$@" to pass remaining arguments correctly, handling spaces in values
    if ! vault kv put -address="${VAULT_ADDR}" "${path}" "$@"; then
        echo "Seeder: Error writing to Vault path: $path" >&2
        exit 1
    fi
}

# Note: We use the environment variables sourced from the .env file directly.

# --- CORS ---
vault_put "${BASE_VAULT_PATH}/cors" \
    "allowed-origins=${CORS_ALLOWED_ORIGINS}"
#    "allowed-methods=${CORS_ALLOWED_METHODS}" \
#    "allowed-headers=${CORS_ALLOWED_HEADERS}" \
#    "exposed-headers=${CORS_EXPOSED_HEADERS}" \
#    "allow-credentials=${CORS_ALLOW_CREDENTIALS}" \
#    "max-age=${CORS_MAX_AGE}"

# --- Database Secrets ---
vault_put "${BASE_VAULT_PATH}/database" \
    "password=${DB_PASSWORD}" \
    "username=${DB_USERNAME}"
    # Add username, url etc. if they are also in Vault

# --- DeviceFingerprint Secrets ---
vault_put "${BASE_VAULT_PATH}/device-fingerprint" \
    "salt=${DEVICE_FINGERPRINT_SALT}"
    # Add other fields if needed

# --- Email ---
vault_put "${BASE_VAULT_PATH}/email" \
    "team-email=${TEAM_EMAIL}" \
    "alert-email-sender=${ALERT_EMAIL_SENDER}" \
    "notification-email-to=${NOTIFICATION_EMAIL_TO}" \
    "notification-email-from=${NOTIFICATION_EMAIL_FROM}" \
    "email-host=${MAIL_HOST}" \
    "email-username=${MAIL_USERNAME}" \
    "email-password=${MAIL_PASSWORD}" \
    "email-port=${MAIL_PORT}" \
    "email-starttls-enable=${MAIL_STARTTLS_ENABLE}" \
    "email-auth-enable=${MAIL_AUTH_ENABLE}"

# --- GeoIP2 Secrets ---
vault_put "${BASE_VAULT_PATH}/geoip2" \
    "account-id=${GEOIP2_ACCOUNT_ID}" \
    "license-key=${GEOIP2_LICENSE_KEY}"

    # Add GEOIP2 fields if they are separate

# --- Hashing ---
vault_put "${BASE_VAULT_PATH}/hashing" \
    "algorithm=${HASHING_ALGORITHM}" \
    "iterations=${HASHING_ITERATIONS}" \
    "key-length=${HASHING_KEY_LENGTH}" \
    "encoding=${HASH_ENCODING}"
#    "pepper=${HASHING_PEPPER}" \
#    "salt=${HASHING_SALT}" \

# --- MFA Secrets ---
vault_put "${BASE_VAULT_PATH}/mfa" \
    "encryption-password=${MFA_ENCRYPTION_PASSWORD}" \
    "encryption-salt=${MFA_ENCRYPTION_SALT}"

# --- Observability Secrets ---
vault_put "${BASE_VAULT_PATH}/observability" \
    "loki-url=${LOKI_URL}"
#    "sentry-dsn=${SENTRY_DSN}" \
#    "sentry-environment=${SENTRY_ENVIRONMENT}" \
#    "sentry-release=${SENTRY_RELEASE}" \
#    "sentry-traces-sample-rate=${SENTRY_TRACES_SAMPLE_RATE}" \
#    "sentry-debug=${SENTRY_DEBUG}" \
#    "sentry-attach-stacktrace=${SENTRY_ATTACH_STACKTRACE}"

# --- PASETO Secrets ---
# The field names (private-key, public-key, kid, secret-key) are specified here.

# Public Access
vault_put "${BASE_VAULT_PATH}/paseto-public-access" \
    "private-key=${PASETO_PUBLIC_ACCESS_PRIVATE_KEY}" \
    "public-key=${PASETO_PUBLIC_ACCESS_PUBLIC_KEY}" \
    "kid=${PASETO_PUBLIC_ACCESS_KID}"

# Local Access
vault_put "${BASE_VAULT_PATH}/paseto-local-access" \
    "secret-key=${PASETO_LOCAL_ACCESS_SECRET_KEY}" \
    "kid=${PASETO_LOCAL_ACCESS_KID}"

# Local Refresh
vault_put "${BASE_VAULT_PATH}/paseto-local-refresh" \
    "secret-key=${PASETO_LOCAL_REFRESH_SECRET_KEY}" \
    "kid=${PASETO_LOCAL_REFRESH_KID}"

# Public Refresh (Uncomment if used)
# vault_put "${BASE_VAULT_PATH}/paseto-public-refresh" \
#     "private-key=${PASETO_PUBLIC_REFRESH_PRIVATE_KEY}" \
#     "public-key=${PASETO_PUBLIC_REFRESH_PUBLIC_KEY}" \
#     "kid=${PASETO_PUBLIC_REFRESH_KID}"

#  --- reCAPTCHA Secrets ---
 vault_put "${BASE_VAULT_PATH}/recaptcha" \
     "secret=${RECAPTCHA_SECRET}" \
      "site-key=${RECAPTCHA_SITE_KEY}"

# --- Slack variables ---
vault_put "${BASE_VAULT_PATH}/slack" \
    "bot-token=${SLACK_BOT_TOKEN}" \
    "channel=${SLACK_CHANNEL}" \
    "channel-name=${SLACK_CHANNEL_NAME}" \
    "api-token=${SLACK_API_TOKEN}" \
    "webhook-url=${SLACK_WEBHOOK_URL}" \

# --- Valkey Secrets ---
vault_put "${BASE_VAULT_PATH}/valkey" \
    "password=${VALKEY_PASSWORD}" \
    "valkey_HOST=${VALKEY_HOST}" \
    "valkey_PORT=${VALKEY_PORT}" \

# --- Whois Secrets ---
vault_put "${BASE_VAULT_PATH}/whois" \
    "server=${WHOIS_SERVER}" \
    "port=${WHOIS_PORT}" \
    "timeout=${WHOIS_TIMEOUT}" \

# --- Add other secrets as needed ---
# Example: Recaptcha
# vault_put "${BASE_VAULT_PATH}/recaptcha" \
#     "secret=${RECAPTCHA_SECRET}"

# --- REMOVED the old generic put command ---
# put_args=""
# ... (old parsing logic removed) ...
# if [ -n "$put_args" ]; then
#   eval "vault kv put -address=${VAULT_ADDR} ${VAULT_PATH} ${put_args}"
#   ...
# fi

echo "Seeder: Vault population script finished successfully."
exit 0 # Important: Exit successfully
#!/bin/sh
# Seeder script for Vault
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
# BASE_VAULT_PATH is the prefix *before* /data/
BASE_VAULT_PATH_PREFIX="${VAULT_PATH:-secret/amlume-shop/local}"
ENV_FILE_PATH="${ENV_FILE_PATH:-/.env}"
VAULT_READY_TIMEOUT="${VAULT_READY_TIMEOUT:-60}"

echo "Seeder: Starting up."
echo "Seeder: Target Vault Addr: ${VAULT_ADDR}"
echo "Seeder: Target Base Vault Path Prefix: $BASE_VAULT_PATH_PREFIX" # Log base path prefix
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
  # Use curl's exit code directly
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
ls -l "$ENV_FILE_PATH" || { echo "Seeder: Error - Env file not found at $ENV_FILE_PATH"; exit 1; }
if [ ! -r "$ENV_FILE_PATH" ]; then
    echo "Seeder: Error - Env file is not readable at $ENV_FILE_PATH"
    exit 1
fi
set -a
# shellcheck source=/dev/null
. "$ENV_FILE_PATH"
set +a
echo "Seeder: Environment variables sourced."
# --- DEBUGGING ECHO STATEMENTS (Keep for now) ---
echo "DEBUG: DB_PASSWORD length: ${#DB_PASSWORD}"
echo "DEBUG: PASETO_PUBLIC_ACCESS_PRIVATE_KEY length: ${#PASETO_PUBLIC_ACCESS_PRIVATE_KEY}"
echo "DEBUG: CORS_ALLOWED_ORIGINS: ${CORS_ALLOWED_ORIGINS}"
# --- END DEBUGGING ---

# --- Populate Vault with Specific Secrets ---
echo "Seeder: Populating specific secrets into Vault..."

# Function to write secrets using curl and KV v2 API
# Takes the secret name (e.g., "cors", "database") as $1
# Takes key=value pairs as subsequent arguments ($@)
vault_put_curl() {
    secret_name="$1"
    shift # Remove secret name from arguments

        # Separate the mount point (first part of BASE_VAULT_PATH_PREFIX) from the logical path
        VAULT_MOUNT=$(echo "$BASE_VAULT_PATH_PREFIX" | cut -d'/' -f1)
        VAULT_LOGICAL_PATH=$(echo "$BASE_VAULT_PATH_PREFIX" | cut -d'/' -f2-) # Get the rest of the path

        # Construct the correct KVv2 API path: <mount>/data/<logical_path>/<secret_name>
        # Handle cases where VAULT_LOGICAL_PATH might be empty (though not in our current setup)
        if [ -z "$VAULT_LOGICAL_PATH" ]; then
          api_path="${VAULT_MOUNT}/data/${secret_name}"
        else
          api_path="${VAULT_MOUNT}/data/${VAULT_LOGICAL_PATH}/${secret_name}"
        fi

        api_url="${VAULT_ADDR}/v1/${api_path}"

        echo "Seeder: Writing via curl to Corrected API URL: $api_url" # Log the corrected URL

    # Build the JSON data payload: {"data": {"key1":"value1", "key2":"value2"}}
    json_data_pairs=""
    first_pair=true
    for arg in "$@"; do
        # Split arg at the first '='
#        key=$(echo "$arg" | cut -d'=' -f1)
#        value=$(echo "$arg" | cut -d'=' -f2-)
        key="${arg%%=*}"
        value="${arg#*=}"


        # Basic JSON escaping for the value (handle double quotes and backslashes)
        # This is a simplified escaping, might need improvement for complex values
        escaped_value=$(echo "$value" | sed 's/\\/\\\\/g; s/"/\\"/g')

        if [ "$first_pair" = true ]; then
            json_data_pairs="\"$key\":\"$escaped_value\""
            first_pair=false
        else
            json_data_pairs="$json_data_pairs,\"$key\":\"$escaped_value\""
        fi
    done

    json_payload="{\"data\":{$json_data_pairs}}"
    # echo "DEBUG: JSON Payload: $json_payload" # Uncomment for payload debugging

    # (Keep the variable setup before this block)

    # Make the API call using curl
    # -f: fail fast (exit non-zero on HTTP error >= 400)
    # -w: write out custom format (http_code) to stdout
    # Capture stdout (status code) and stderr separately
    # Store curl's exit code ($?) immediately after execution

    # --- Temporarily disable exit on error ---
#    set +e
    # Execute curl, capturing status code to a variable, redirecting body/stderr
    http_status_output=$(curl -f -w "%{http_code}" \
         --request POST \
         --header "X-Vault-Token: ${VAULT_TOKEN}" \
         --header "Content-Type: application/json" \
         --data "$json_payload" \
         "$api_url" \
         --output /tmp/curl_response.txt \
         --stderr /tmp/curl_stderr.txt)
    curl_exit_code=$? # Capture exit code IMMEDIATELY
    # --- Re-enable exit on error ---
#    set -e

    # Check curl's exit code first (This block should now execute correctly)
    if [ $curl_exit_code -ne 0 ]; then
        # curl itself failed OR -f caused failure due to HTTP status >= 400
        echo "Seeder: Error writing via curl to API path: $api_path (curl exit code: $curl_exit_code)" >&2

        # Try to determine if it was an HTTP error or other curl failure
        # Check if http_status_output looks like a valid HTTP code (simple check)
        if printf '%s' "$http_status_output" | grep -qE '^[1-5][0-9]{2}$'; then
             echo "Seeder: HTTP Status Code: $http_status_output" >&2
        else
             # If curl failed before getting HTTP status, http_status_output might be empty or just contain curl errors
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
        # curl succeeded (exit code 0), HTTP status must be < 400
        # Log success, including the actual HTTP status returned by Vault (e.g., 200 or 204)
        echo "Seeder: Successfully wrote via curl to API path: $api_path (HTTP Status: $http_status_output)"
        rm -f /tmp/curl_response.txt /tmp/curl_stderr.txt
    fi

}

# --- Call the new function, passing the *secret name* and key=value pairs ---
# Note: The path construction now happens *inside* vault_put_curl

# --- CORS ---
vault_put_curl "cors" \
    "allowed-origins=${CORS_ALLOWED_ORIGINS}"

# --- Database Secrets ---
vault_put_curl "database" \
    "password=${DB_PASSWORD}" \
    "username=${DB_USERNAME}"

# --- DeviceFingerprint Secrets ---
vault_put_curl "device-fingerprint" \
    "salt=${DEVICE_FINGERPRINT_SALT}"

# --- Email ---
vault_put_curl "email" \
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
vault_put_curl "geoip2" \
    "account-id=${GEOIP2_ACCOUNT_ID}" \
    "license-key=${GEOIP2_LICENSE_KEY}"

# --- Hashing ---
vault_put_curl "hashing" \
    "algorithm=${HASHING_ALGORITHM}" \
    "iterations=${HASHING_ITERATIONS}" \
    "key-length=${HASHING_KEY_LENGTH}" \
    "encoding=${HASH_ENCODING}"

# --- MFA Secrets ---
vault_put_curl "mfa" \
    "encryption-password=${MFA_ENCRYPTION_PASSWORD}" \
    "encryption-salt=${MFA_ENCRYPTION_SALT}"

# --- Observability Secrets ---
vault_put_curl "observability" \
    "loki-url=${LOKI_URL}"

# --- PASETO Secrets ---
# Public Access
vault_put_curl "paseto-pub-access" \
    "private-key=${PASETO_PUBLIC_ACCESS_PRIVATE_KEY}" \
    "public-key=${PASETO_PUBLIC_ACCESS_PUBLIC_KEY}" \
    "kid=${PASETO_PUBLIC_ACCESS_KID}"

# Local Access
vault_put_curl "paseto-local-access" \
    "secret-key=${PASETO_LOCAL_ACCESS_SECRET_KEY}" \
    "kid=${PASETO_LOCAL_ACCESS_KID}"

# Local Refresh
vault_put_curl "paseto-local-refresh" \
    "secret-key=${PASETO_LOCAL_REFRESH_SECRET_KEY}" \
    "kid=${PASETO_LOCAL_REFRESH_KID}"

# --- reCAPTCHA Secrets ---
vault_put_curl "recaptcha" \
     "secret=${RECAPTCHA_SECRET}" \
     "site-key=${RECAPTCHA_SITE_KEY}"

# --- Slack variables ---
vault_put_curl "slack" \
    "bot-token=${SLACK_BOT_TOKEN}" \
    "channel=${SLACK_CHANNEL}" \
    "channel-name=${SLACK_CHANNEL_NAME}" \
    "api-token=${SLACK_API_TOKEN}" \
    "webhook-url=${SLACK_WEBHOOK_URL}"

# --- Valkey Secrets ---
vault_put_curl "valkey" \
    "password=${VALKEY_PASSWORD}" \
    "host=${VALKEY_HOST}" \
    "port=${VALKEY_PORT}"

# --- Whois Secrets ---
vault_put_curl "whois" \
    "server=${WHOIS_SERVER}" \
    "port=${WHOIS_PORT}" \
    "timeout=${WHOIS_TIMEOUT}"

# --- Add other secrets as needed ---

echo "Seeder: Vault population script finished successfully."
exit 0
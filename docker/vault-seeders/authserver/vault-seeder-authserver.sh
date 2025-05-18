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

VAULT_ADDR="${VAULT_ADDR:-http://vault:8200}"
VAULT_TOKEN="${VAULT_TOKEN}"
# *** CHANGE THIS PATH FOR AUTHSERVER ***
BASE_VAULT_PATH_PREFIX="${VAULT_PATH:-secret/authserver/local}" # Logical path for authserver
ENV_FILE_PATH="${ENV_FILE_PATH:-/app/authserver.env}" # Path inside the seeder container
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


echo "Authserver Seeder: Starting up."
echo "Authserver Seeder: Target Vault Addr: ${VAULT_ADDR}"
echo "Authserver Seeder: Target Vault Path (Logical): $BASE_VAULT_PATH_PREFIX"
echo "Authserver Seeder: Source Env File: $ENV_FILE_PATH"
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

# --- Construct the SINGLE JSON Payload for Authserver ---
echo "Authserver Seeder: Constructing JSON payload for Vault..."
JSON_DATA_PAIRS=""
add_pair() {
   key="$1"
   value="$2"
   escaped_value=$(echo "$value" | sed -e ':a' -e 'N' -e '$!ba' -e 's/\\/\\\\/g; s/"/\\"/g; s/\n/\\n/g')
   if [ -z "$JSON_DATA_PAIRS" ]; then
       JSON_DATA_PAIRS="\"$key\":\"$escaped_value\""
   else
       JSON_DATA_PAIRS="$JSON_DATA_PAIRS,\"$key\":\"$escaped_value\""
   fi
}

# --- Add Authserver-specific secrets with correct Spring property names ---
# These keys MUST match what authserver's application-local.yml expects
# or how Spring Cloud Vault maps them.

# Database credentials for authserver
add_pair "spring.datasource.username" "${AUTH_DB_USER}"
add_pair "spring.datasource.password" "${AUTH_DB_PASSWORD}"

# OAuth2 client credentials for Google/GitHub (used by authserver to log its users in)
add_pair "spring.security.oauth2.client.registration.google.client-id" "${GOOGLE_CLIENT_ID_FOR_AUTHSERVER}"
add_pair "spring.security.oauth2.client.registration.google.client-secret" "${GOOGLE_CLIENT_SECRET_FOR_AUTHSERVER}"
add_pair "spring.security.oauth2.client.registration.github.client-id" "${GITHUB_CLIENT_ID_FOR_AUTHSERVER}"
add_pair "spring.security.oauth2.client.registration.github.client-secret" "${GITHUB_CLIENT_SECRET_FOR_AUTHSERVER}"

# Remember-me key for authserver
add_pair "spring.security.rememberme.key" "${AUTHSERVER_REMEMBER_ME_KEY}"

# Secrets for OAuth2 clients that authserver itself defines and manages
# These keys match the @Value annotations in authserver's LocalSecurityConfig
add_pair "oauth2.clients.amlumeapi.secret" "${OAUTH2_CLIENT_AMLUMEAPI_SECRET_AS}"
add_pair "oauth2.clients.amlumeintrospect.secret" "${OAUTH2_CLIENT_AMLUMEINTROSPECT_SECRET_AS}"
add_pair "oauth2.clients.shopClient.secret" "${OAUTH2_CLIENT_SHOPCLIENT_SECRET_AS}" # For amlume-shop client
add_pair "oauth2.clients.postmanClient.secret" "${OAUTH2_CLIENT_POSTMANCLIENT_SECRET_AS}" # For postman client

# (Optional) Authserver Truststore Password
# add_pair "app.ssl.trust-store.password" "${AUTHSERVER_APP_CENTRAL_TRUSTSTORE_PASSWORD}"

JSON_PAYLOAD="{\"data\":{$JSON_DATA_PAIRS}}"
# echo "DEBUG Authserver Payload: $JSON_PAYLOAD"

# --- Write the SINGLE Payload to Authserver's Vault Path ---
echo "Authserver Seeder: Writing all secrets to Vault path $BASE_VAULT_PATH_PREFIX..."
VAULT_MOUNT=$(echo "$BASE_VAULT_PATH_PREFIX" | cut -d'/' -f1)
VAULT_LOGICAL_PATH=$(echo "$BASE_VAULT_PATH_PREFIX" | cut -d'/' -f2-)
API_PATH="${VAULT_MOUNT}/data/${VAULT_LOGICAL_PATH}"
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

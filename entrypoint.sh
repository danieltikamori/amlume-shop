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
#VAULT_ADDR="${VAULT_ADDR:-http://vault:8200}" # Default to service name 'vault'
VAULT_ADDR="${VAULT_ADDR:-http://vault:8200}" # Default to service name 'vault'
VAULT_TOKEN="${VAULT_TOKEN}"
VAULT_PATH="${VAULT_PATH:-secret/amlume-shop/mfa}" # Default path, can be overridden
ENV_FILE_PATH="${ENV_FILE_PATH:-/.env}"          # Default path where .env is mounted
VAULT_READY_TIMEOUT="${VAULT_READY_TIMEOUT:-60}" # Max seconds to wait for Vault

echo "Seeder: Starting up."
echo "Seeder: Target Vault Addr: $VAULT_ADDR"
echo "Seeder: Target Vault Path: $VAULT_PATH"
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
  # Check Vault health endpoint (unauthenticated)
  # Adjust if your Vault dev instance requires TLS or different path
  if curl --fail --silent --show-error "$VAULT_ADDR/v1/sys/health" | jq -e '.initialized == true and .sealed == false and .standby == false' > /dev/null; then
    echo "Seeder: Vault is initialized, unsealed, and active."
    break
  fi

  current_time=$(date +%s)
  elapsed_time=$((current_time - start_time))

  if [ "$elapsed_time" -ge "$VAULT_READY_TIMEOUT" ]; then
    echo "Seeder: Error - Timeout waiting for Vault to become ready after $VAULT_READY_TIMEOUT seconds."
    # Attempt to get more info if possible
    curl --silent --show-error "$VAULT_ADDR/v1/sys/health" || echo "Seeder: Could not reach Vault health endpoint."
    exit 1
  fi

  echo "Seeder: Vault not ready yet, sleeping for 3 seconds..."
  sleep 3
done

# --- Populate Vault from .env file ---
echo "Seeder: Vault is ready. Populating secrets from $ENV_FILE_PATH into $VAULT_PATH..."

# Export token for Vault CLI commands
export VAULT_TOKEN

# Using awk for slightly more robust parsing than basic shell loops
# Handles comments (#), empty lines, and basic key=value pairs
awk -F= '!/^#/ && NF > 1 {
    key=$1
    # Reconstruct value if it contains '='
    value = substr($0, index($0, "=") + 1)
    # Trim leading/trailing whitespace from key and value (optional)
    gsub(/^[[:space:]]+|[[:space:]]+$/, "", key)
    gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
    # Basic quote removal (optional)
    gsub(/^'\''|'\''$/, "", value)
    gsub(/^\"|\"$/, "", value)

    # Construct the kv put command arguments: key=value
    kv_pair = sprintf("%s=%s", key, value)

    # Execute vault kv put for each pair
    cmd = sprintf("vault kv put -address=%s %s \"%s\"", ENVIRON["VAULT_ADDR"], ENVIRON["VAULT_PATH"], kv_pair)
    print "Seeder: Writing key: " key
    if (system(cmd) != 0) {
        print "Seeder: Error writing key: " key > "/dev/stderr"
        # exit 1 # Optionally exit on first error
    }
}' < "$ENV_FILE_PATH"

echo "Seeder: Vault population script finished."
exit 0 # Important: Exit successfully so Compose knows the one-time task is done
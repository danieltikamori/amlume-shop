# Use an Alpine base image
FROM alpine:latest

# Set Vault version (adjust as needed, check Vault website for current version/hashes)
# https://developer.hashicorp.com/vault/install
# https://releases.hashicorp.com/vault/
# https://releases.hashicorp.com/vault/1.19.2/vault_1.19.2_SHA256SUMS
# https://releases.hashicorp.com/vault/1.19.2/vault_1.19.2_SHA256SUMS.sig perhaps not necessary

ARG VAULT_VERSION=1.19.2
ARG VAULT_SHA256=c6781c3e0ec431f39bcc8f1443d09f3b8944c90c348e91aa13182b4e1fd2797f

# Install prerequisites
RUN apk add --no-cache curl jq openssl

# Download and install Vault CLI
RUN curl -Lo vault.zip "https://releases.hashicorp.com/vault/${VAULT_VERSION}/vault_${VAULT_VERSION}_linux_amd64.zip" && \
    echo "${VAULT_SHA256}  vault.zip" | sha256sum -c && \
    unzip vault.zip && \
    mv vault /usr/local/bin/vault && \
    rm vault.zip

# Create directory for scripts and copy entrypoint script
RUN mkdir /app
COPY entrypoint.sh /app/entrypoint.sh
RUN dos2unix entrypoint.sh # Convert line endings to Unix format if necessary
RUN chmod +x /app/entrypoint.sh

WORKDIR /app

# This entrypoint will run when the container starts
ENTRYPOINT ["/app/entrypoint.sh"]
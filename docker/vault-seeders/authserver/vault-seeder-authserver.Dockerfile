FROM alpine/curl:latest

# Install bash and sed if not present in alpine/curl (curl image usually has sh)
# alpine/curl is very minimal, might need to add bash if your script uses bash-specific features
# RUN apk add --no-cache bash sed

WORKDIR /app

# Copy the authserver-specific seeder script
COPY ./docker/vault-seeders/authserver/vault-seeder-authserver.sh /app/vault-seeder-authserver.sh
RUN chmod +x /app/vault-seeder-authserver.sh

# The .env file will be mounted as a volume in docker-compose

ENTRYPOINT ["/app/vault-seeder-authserver.sh"]

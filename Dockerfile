# --- Build Stage ---
# Use a GraalVM native-image base image for Java 21.
FROM ghcr.io/graalvm/native-image-community:21 AS builder

# Set working directory
WORKDIR /app

# --- REMOVED Locale Installation Block ---
# ... (rest of your locale comments) ...
# --- End of Removed Block ---

# Copy the Maven wrapper and project files
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Optional: Download dependencies first to leverage cache better
# RUN ./mvnw dependency:go-offline -B

# Copy the source code
COPY src ./src

# Give execution rights to the Maven wrapper
RUN chmod +x mvnw

# Define the build argument for the executable name
ARG EXECUTABLE_NAME=amlume-shop

# Build the native executable using the 'native' profile
# Disable Vault AND provide a dummy token for the AOT phase
RUN ./mvnw -Pnative native:compile -DskipTests \
     -Dspring.cloud.vault.enabled=false \
     -Dspring.cloud.vault.token=dummy-token-for-build

# --- Run Stage ---
# Use a minimal base image with glibc suitable for dynamically linked native executables.
FROM gcr.io/distroless/base-debian12

# Set working directory
WORKDIR /app

# Copy the native executable from the builder stage
# Uses the EXECUTABLE_NAME argument passed during the build.
# Ensure the ARG is available in this stage if needed, or use the default/passed value directly.
ARG EXECUTABLE_NAME=amlume-shop # Make ARG available here too if ENTRYPOINT needs it explicitly
COPY --from=builder /app/target/${EXECUTABLE_NAME} ./

# Expose the port your Spring Boot app runs on (default is 8080)
EXPOSE 8080

# Set the entry point to run the native executable
# Uses the EXECUTABLE_NAME argument passed during the build.
ENTRYPOINT ["/app/${EXECUTABLE_NAME}"]

# Optional: Add healthcheck if needed
# HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 CMD curl -f http://localhost:8080/actuator/health || exit 1
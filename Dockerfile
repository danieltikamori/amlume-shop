# --- Build Stage ---
# Use a GraalVM native-image base image for Java 21.
FROM ghcr.io/graalvm/native-image-community:21 AS builder

# Set working directory
WORKDIR /app

# Install locales and set default locale (Fixes setlocale warnings)
# Ensure your base image uses apt (like Debian/Ubuntu based ones)
# If using a different package manager (like apk for Alpine), adjust accordingly.
RUN apt-get update && apt-get install -y locales \
    && echo "en_US.UTF-8 UTF-8" >> /etc/locale.gen \
    && locale-gen en_US.UTF-8 \
    && update-locale LANG=en_US.UTF-8 \
    && rm -rf /var/lib/apt/lists/*
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8

# Copy the Maven wrapper and project files
# Copy pom.xml and mvnw first to leverage Docker cache
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Optional: Download dependencies first to leverage cache better
# RUN ./mvnw dependency:go-offline -B

# Copy the source code
COPY src ./src

# Give execution rights to the Maven wrapper
RUN chmod +x mvnw

# Define the build argument for the executable name (default is a placeholder)
# This MUST be overridden during the 'docker build' command.
# The actual name is typically the <artifactId> from pom.xml (likely 'amlume-shop').
ARG EXECUTABLE_NAME=amlume-shop

# Build the native executable using the 'native' profile
# Ensure your pom.xml has the native-maven-plugin configured correctly in the 'native' profile.
RUN ./mvnw -Pnative native:compile -DskipTests

# --- Run Stage ---
# Use a minimal base image with glibc suitable for dynamically linked native executables.
FROM gcr.io/distroless/base-debian12

# Set working directory
WORKDIR /app

# Copy the native executable from the builder stage
# Uses the EXECUTABLE_NAME argument passed during the build.
COPY --from=builder /app/target/${EXECUTABLE_NAME} ./

# Expose the port your Spring Boot app runs on (default is 8080)
EXPOSE 8080

# Set the entry point to run the native executable
# Uses the EXECUTABLE_NAME argument passed during the build.
ENTRYPOINT ["/app/${EXECUTABLE_NAME}"]
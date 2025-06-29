# Start with a base image that has Java 21
FROM openjdk:21-slim

# Set the working directory inside the container
WORKDIR /app

# --- New Relic APM Agent Setup ---
# Copy the New Relic agent and its configuration file into the image
# Assumes you have a 'newrelic' folder next to your Dockerfile
COPY newrelic/newrelic.jar /app/newrelic/newrelic.jar
COPY newrelic/newrelic.yml /app/newrelic/newrelic.yml
# --- End New Relic Setup ---

# Copy the compiled application JAR file from the target directory
# This assumes you've run 'mvn clean install'
#COPY target/authserver-app-0.0.1-SNAPSHOT.jar app.jar

# Copy the compiled application JAR file
COPY target/authserver-app-*.jar app.jar

# Expose the port the application runs on
EXPOSE 9000

# --- Modified Entrypoint to include the Java Agent ---
# This command will be run when the container starts.
# The -javaagent flag is what attaches New Relic to your application.
ENTRYPOINT ["java", "-javaagent:/app/newrelic/newrelic.jar", "-jar", "app.jar"]

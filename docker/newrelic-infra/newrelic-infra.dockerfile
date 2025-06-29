# Use the official New Relic Infrastructure agent image.
# This image contains all the necessary binaries to run the agent.
FROM newrelic/infrastructure:latest

# Add the configuration file for the infrastructure agent.
# This file should contain the license_key placeholder.
# The actual key will be injected as an environment variable in docker-compose.
ADD newrelic-infra.yml /etc/newrelic-infra.yml

# --- REMOVED ---
# The lines for adding newrelic.jar and its newrelic.yml are incorrect here.
# They belong in the Dockerfile for your actual Java application (e.g., auth-server).
#RUN mkdir -p /usr/local/newrelic
#ADD ./newrelic/newrelic.jar /usr/local/newrelic/newrelic.jar
#ADD ./newrelic/newrelic.yml /usr/local/newrelic/newrelic.yml

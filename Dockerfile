# syntax=docker/dockerfile:1

# ─────────────────────────────────────────────────────────────────────────────
# Stage 1 — build the Spring Boot fat jar
# ─────────────────────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Cache dependencies first
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

# Build
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ─────────────────────────────────────────────────────────────────────────────
# Stage 2 — runtime image with the Datadog Java tracer/agent
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre
WORKDIR /app

# Download dd-java-agent.jar. Datadog docs steer to the version-agnostic redirect
# (https://dtdg.co/latest-java-tracer) rather than pinning a number.
ADD https://dtdg.co/latest-java-tracer /app/dd-java-agent.jar

# App artifact
COPY --from=build /build/target/workforce-vms.jar /app/workforce-vms.jar

# Seed documents for the (path-traversal) attachment endpoint
COPY documents /app/documents

# ── Source Code Integration ─────────────────────────────────────────────────
# Embed git metadata at build time so Code Security findings link to file/line and
# the PR. Pass with:  --build-arg DD_GIT_COMMIT_SHA=$(git rev-parse HEAD) ...
ARG DD_GIT_REPOSITORY_URL
ARG DD_GIT_COMMIT_SHA
ENV DD_GIT_REPOSITORY_URL=${DD_GIT_REPOSITORY_URL}
ENV DD_GIT_COMMIT_SHA=${DD_GIT_COMMIT_SHA}

EXPOSE 8080

# The dd-java-agent is attached here; all DD_* runtime toggles (APM/IAST/SCA/AAP)
# are supplied by docker-compose.yml so they are easy to show and change live.
ENTRYPOINT ["java", "-javaagent:/app/dd-java-agent.jar", "-jar", "/app/workforce-vms.jar"]

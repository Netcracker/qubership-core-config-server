# Multi-stage Dockerfile for Spring application
# Stage 1: Build the Spring application
FROM maven:3.9.10-eclipse-temurin-21-alpine AS build

# Set working directory
WORKDIR /app

# Copy the parent pom.xml first for better layer caching
COPY pom.xml ./

# Copy modules pom.xml
COPY config-server-app/pom.xml ./config-server-app/ &&
COPY config-server-core/pom.xml ./config-server-core/ &&
COPY config-server-report-aggregator/pom.xml ./config-server-report-aggregator/

RUN --mount=type=secret,id=github-username \
    --mount=type=secret,id=github-token \
    --mount=type=cache,target=/root/.m2 \
    mkdir -p /root/.m2 && \
    echo "<settings>\
      <servers>\
        <server>\
          <id>github</id>\
          <username>$(cat /run/secrets/github-username)</username>\
          <password>$(cat /run/secrets/github-token)</password>\
        </server>\
      </servers>\
    </settings>" > /root/.m2/settings.xml && \
    mvn dependency:go-offline -B -q

# Copy source code
COPY config-server-app/src ./config-server-app/src &&
COPY config-server-core/src ./config-server-core/src &&
COPY config-server-report-aggregator/src ./config-server-report-aggregator/src

# Build the application
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -B -q

#---------------------------------------------------------
# Stage 2: Runtime image
FROM ghcr.io/netcracker/qubership/java-base:1.0.0
LABEL maintainer="qubership"

# Set working directory
WORKDIR /app

# Copy the built application from the build stage
COPY --from=build --chown=10001:0 /app/config-server-app/target/config-server-app-*.jar /app/config-server.jar

# Switch to non-root user
USER 10001:0

EXPOSE 8080


CMD ["/usr/bin/java", "-Xmx512m", "-jar", "/app/config-server.jar"]
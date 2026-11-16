FROM ghcr.io/netcracker/qubership-java-base:21-alpine-2.5.13@sha256:ea4f438afd668fb01996481a4475fddcff8b64c2d771288da7d206e2e202d024
LABEL maintainer="qubership"

COPY --chown=10001:0 config-server-app/target/config-server-app-*.jar /app/config-server.jar

EXPOSE 8080

WORKDIR /app

CMD ["/usr/bin/java", "-Xmx512m", "-jar", "/app/config-server.jar"]

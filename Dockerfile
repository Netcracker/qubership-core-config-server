FROM ghcr.io/netcracker/qubership-java-base:21-alpine-2.5.14@sha256:d43eabd2780958e0cc031c78ed534f382918b4e6e06a9ddb785688694aa1c7b0
LABEL maintainer="qubership"

COPY --chown=10001:0 config-server-app/target/config-server-app-*.jar /app/config-server.jar

EXPOSE 8080

WORKDIR /app

CMD ["/usr/bin/java", "-Xmx512m", "-jar", "/app/config-server.jar"]

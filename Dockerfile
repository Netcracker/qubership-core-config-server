FROM ghcr.io/netcracker/qubership-java-base:21-alpine-2.5.9@sha256:d2715fc7372feb9fb5f35e65d20a5aed412c20aba0b046f3f74656050309c940
LABEL maintainer="qubership"

COPY --chown=10001:0 config-server-app/target/config-server-app-*.jar /app/config-server.jar

EXPOSE 8080

WORKDIR /app

CMD ["/usr/bin/java", "-Xmx512m", "-jar", "/app/config-server.jar"]

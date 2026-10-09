FROM ghcr.io/netcracker/qubership-java-base:21-alpine-2.5.7@sha256:36b4a4889f78d9e48e6afb71b6493aa6d5ca3295928f958e7a906286f8613ef0
LABEL maintainer="qubership"

COPY --chown=10001:0 config-server-app/target/config-server-app-*.jar /app/config-server.jar

EXPOSE 8080

WORKDIR /app

CMD ["/usr/bin/java", "-Xmx512m", "-jar", "/app/config-server.jar"]

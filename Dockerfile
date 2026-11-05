FROM ghcr.io/netcracker/qubership-java-base:21-alpine-2.5.11@sha256:c9019537572ef7b09613a3352739fa6d2b7c6c3b8e63e6a3312d77595dac6c88
LABEL maintainer="qubership"

COPY --chown=10001:0 config-server-app/target/config-server-app-*.jar /app/config-server.jar

EXPOSE 8080

WORKDIR /app

CMD ["/usr/bin/java", "-Xmx512m", "-jar", "/app/config-server.jar"]

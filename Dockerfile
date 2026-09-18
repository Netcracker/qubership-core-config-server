FROM ghcr.io/netcracker/qubership-java-base:21-alpine-2.5.4@sha256:e2c93e22e265a36801183291e9825ba6969b8281f96a043f4adb3fce05368abc
LABEL maintainer="qubership"

COPY --chown=10001:0 config-server-app/target/config-server-app-*.jar /app/config-server.jar

EXPOSE 8080

WORKDIR /app

CMD ["/usr/bin/java", "-Xmx512m", "-jar", "/app/config-server.jar"]

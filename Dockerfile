FROM ghcr.io/netcracker/qubership/java-base:1.2.0
LABEL maintainer="qubership"

COPY --chown=10001:0 config-server-app/target/config-server-app-*.jar /app/config-server.jar

EXPOSE 8080

WORKDIR /app
USER 10001:10001

CMD ["/usr/bin/java", "-Xmx512m", "-jar", "/app/config-server.jar"]
